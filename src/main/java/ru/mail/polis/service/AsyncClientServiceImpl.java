package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;
import static ru.mail.polis.service.ServiceImpl.getConfig;
import static ru.mail.polis.util.Util.HANDLE_ME;

public class AsyncClientServiceImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncClientServiceImpl.class);

    private enum ErrorNames {
        IO_ERROR, NOT_ALLOWED_METHOD_ERROR, CANNOT_SEND, MOVED, INVALID_KEY,
        DUPLICATE_NODES, BAD_REPLICAS, FUTURE_ERROR, TERMINATION_ERROR
    }

    private static final Map<AsyncClientServiceImpl.ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(AsyncClientServiceImpl.ErrorNames.IO_ERROR, "IO exception raised"),
            entry(AsyncClientServiceImpl.ErrorNames.NOT_ALLOWED_METHOD_ERROR, "Method not allowed"),
            entry(ErrorNames.CANNOT_SEND, "Response can't be sent {}"),
            entry(ErrorNames.MOVED, "310 Removed Permanently"),
            entry(ErrorNames.INVALID_KEY, "Key is empty"),
            entry(ErrorNames.DUPLICATE_NODES, "Found multiple nodes with same ID"),
            entry(ErrorNames.BAD_REPLICAS, "Bad replicas format"),
            entry(ErrorNames.FUTURE_ERROR, "Error resolving a future"),
            entry(ErrorNames.TERMINATION_ERROR, "Couldn't terminate executor")
    );

    @NotNull
    private final DAO dao;
    private final ExecutorService executorService;
    private final Topology topology;

    private final Map<String, HttpClient> nodesToClients;
    private final ReplicationFactor rf;
    private final String deletedMarker;

    /**
     * Service implementation with async HTTP client.
     *
     * @param port           - port server to run on
     * @param dao            - rocksDB impl
     * @param workerPoolSize - workers count
     * @param queueSize      - ThreadPoolExecutor queue size
     * @param topology       - modular topology
     * @throws IOException - throws IO exception
     */
    AsyncClientServiceImpl(
            final int port, @NotNull final DAO dao, final int workerPoolSize,
            final int queueSize, @NotNull final Topology topology
    ) throws IOException {
        super(getConfig(port));
        this.dao = dao;
        this.topology = topology;
        this.executorService = new ThreadPoolExecutor(
                workerPoolSize, workerPoolSize, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("async_workers-%d").build()
        );

        this.deletedMarker = UUID.randomUUID().toString();

        this.nodesToClients = new HashMap<>();
        this.rf = new ReplicationFactor(topology.getSize() / 2 + 1, topology.getSize());

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1));

        for (final String node : topology.getNodes()) {

            if (topology.isSelfId(node)) {
                continue;
            }

            final HttpClient httpClient = httpClientBuilder.build();
            if (nodesToClients.put(node, httpClient) != null) {
                throw new IllegalStateException(MESSAGE_MAP.get(ErrorNames.DUPLICATE_NODES));
            }
        }
    }

    /**
     * Handle GET, PUT & DELETE requests.
     *
     * @param id          -  id.
     * @param httpSession - HTTP session.
     * @param req         - sent request.
     **/
    @Path("/v0/entity")
    @RequestMethod({Request.METHOD_GET, Request.METHOD_PUT, Request.METHOD_DELETE})
    public void entity(
            @Param(value = "id", required = true) final String id, @NotNull final Request req,
            @NotNull final HttpSession httpSession, @Param("replicas") final String replicas
    ) {
        if (id.isEmpty()) {
            sendResponse(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        if (req.getParameter(HANDLE_ME) != null) {
            final CompletableFuture<Response> response = handleLocal(id, req);

            try {
                sendResponse(httpSession, response.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error(MESSAGE_MAP.get(ErrorNames.CANNOT_SEND), e);
            }
            return;
        }

        final ReplicationFactor replicationFactor;
        try {
            replicationFactor = ReplicationFactor.getReplicationFactor(replicas, rf, httpSession);
        } catch (IOException e) {
            logger.error(MESSAGE_MAP.get(ErrorNames.BAD_REPLICAS));
            return;
        }

        handle(id, replicationFactor, httpSession, req);
    }

    private CompletableFuture<Response> proxy(final String to, final Request request) {
        final HttpRequest req = Util.convertRequest(request, to);
        final HttpClient client = nodesToClients.get(to);
        final HttpResponse.BodyHandler<byte[]> body = HttpResponse.BodyHandlers.ofByteArray();

        return client.sendAsync(req, body)
                .thenApply(Util::convertResponse)
                .exceptionally(ex -> new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }

    private void handle(
            final @Param(value = "id", required = true) String key,
            @NotNull final ReplicationFactor replicas,
            @NotNull final HttpSession session,
            final @Param("request") Request request
    ) {
        final String[] nodes = topology.getReplicas(
                ByteBuffer.wrap(key.getBytes(Charset.defaultCharset())), replicas.getFrom()
        );
        final CompletableFuture<?>[] results = new CompletableFuture<?>[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            final String node = nodes[i];
            if (topology.isSelfId(node)) {
                results[i] = handleLocal(key, request);
            } else {
                results[i] = proxy(node, Util.markTargetRequest(request));
            }
        }

        final CompletableFuture<Void> futures = CompletableFuture.allOf(results);
        futures.thenApply(ignored -> new ResponseAggregator()).thenAccept(aggregator -> {
            for (final CompletableFuture<?> future : results) {
                final Response response;
                try {
                    response = (Response) future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR));
                    continue;
                }
                aggregator.add(response, replicas.getAck());
            }
            final Response result = aggregator.getResult();
            sendResponse(session, result);
        }).exceptionally((ex) -> {
            logger.error(MESSAGE_MAP.get(ErrorNames.CANNOT_SEND), ex);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return null;
        });
    }

    private CompletableFuture<Response> handleLocal(
            final @Param(value = "id", required = true) String key,
            final @Param("request") Request request
    ) {
        return CompletableFuture.supplyAsync(() -> {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    return get(key);
                case Request.METHOD_PUT:
                    return put(key, request);
                case Request.METHOD_DELETE:
                    return delete(key);
                default:
                    return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }, executorService);
    }

    /**
     * Handles GET requests.
     *
     * @param key - key to search
     * @return Response
     */
    private Response get(final String key) {
        try {
            if (key.isEmpty()) {
                logger.info(MESSAGE_MAP.get(ErrorNames.INVALID_KEY));
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            final ByteBuffer response = dao.get(ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8)));
            final byte[] result = Util.toByteArray(response);

            if (Arrays.equals(result, this.deletedMarker.getBytes(StandardCharsets.UTF_8))) {
                return new Response(MESSAGE_MAP.get(ErrorNames.MOVED), Response.EMPTY);
            }

            return Response.ok(result);
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ex) {
            logger.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Handles PUT requests.
     *
     * @param key     - key to upsert
     * @param request - user request
     * @return Response
     */
    private Response put(final String key, final Request request) {
        try {
            if (key.isEmpty()) {
                logger.info(MESSAGE_MAP.get(ErrorNames.INVALID_KEY));
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8)),
                    ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ex) {
            logger.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Handles DELETE requests.
     *
     * @param key - key to delete
     * @return Response
     */
    private Response delete(final String key) {
        try {
            if (key.isEmpty()) {
                logger.info(MESSAGE_MAP.get(ErrorNames.INVALID_KEY));
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(
                    Util.toByteBuffer(key),
                    Util.toByteBuffer(this.deletedMarker)
            );
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException ex) {
            logger.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void sendResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException ex) {
            logger.error(MESSAGE_MAP.get(ErrorNames.CANNOT_SEND), ex);
        }
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        sendResponse(session, new Response(Response.OK, Response.EMPTY));
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) {
        sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(MESSAGE_MAP.get(ErrorNames.TERMINATION_ERROR));
            Thread.currentThread().interrupt();
        }
    }
}
