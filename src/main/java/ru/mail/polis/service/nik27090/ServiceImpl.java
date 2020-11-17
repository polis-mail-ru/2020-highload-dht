package ru.mail.polis.service.nik27090;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodeToClient;
    @NotNull
    private final DaoHelper daoHelper;
    @NotNull
    private final HttpHelper httpHelper;
    @NotNull
    private final java.net.http.HttpClient httpClient;

    /**
     * Service constructor.
     *
     * @param port          - port
     * @param dao           - key-value database
     * @param workers       - count of workers
     * @param queueCapacity - ArrayBlockingQueue capacity
     * @throws IOException - extend exception from HttpServer constructor
     */
    public ServiceImpl(
            final int port,
            final @NotNull DAO dao,
            final int workers,
            final int queueCapacity,
            final Duration timeout,
            @NotNull final Topology<String> topology) throws IOException {
        super(createConfig(port));
        this.topology = topology;
        this.nodeToClient = new HashMap<>();
        this.httpHelper = new HttpHelper();
        this.daoHelper = new DaoHelper(dao);

        for (final String node : topology.all()) {
            if (topology.isCurrentNode(node)) {
                continue;
            }

            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=" + timeout.toMillis()));
            if (nodeToClient.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }
        this.executorService = new ThreadPoolExecutor(
                workers, queueCapacity,
                60_000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Exception {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
        this.httpClient =
                java.net.http.HttpClient.newBuilder()
                        .executor(executorService)
                        .connectTimeout(timeout)
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .build();
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        log.debug("Request status.");

        httpHelper.sendResponse(session, Response.ok("OK"));
    }

    /**
     * Get/Delete/Put data by key.
     *
     * @param id      - key for storage
     * @param session - session
     * @param request - request
     */
    @Path("/v0/entity")
    @RequestMethod({METHOD_GET, METHOD_DELETE, METHOD_PUT})
    @SuppressWarnings("FutureReturnValueIgnored")
    public void entityHandler(
            @NotNull final @Param(value = "id", required = true) String id,
            @NotNull final @Param(value = "replicas") String af,
            @NotNull final HttpSession session,
            @NotNull final Request request) {
        CompletableFuture.runAsync(() -> {
            final AckFrom ackFrom = topology.parseAckFrom(af);
            if (ackFrom.getAck() > ackFrom.getFrom() || ackFrom.getAck() <= 0) {
                httpHelper.sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }

            if (id.isEmpty()) {
                httpHelper.sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }
            switch (request.getMethod()) {
                case METHOD_GET:
                    log.debug("GET request: id = {}", id);
                    getEntityExecutor(id, session, request, ackFrom);
                    break;
                case METHOD_PUT:
                    log.debug("PUT request: id = {}, value length = {}", id, request.getBody().length);
                    putEntityExecutor(id, session, request, ackFrom);
                    break;
                case METHOD_DELETE:
                    log.debug("DELETE request: id = {}", id);
                    deleteEntityExecutor(id, session, request, ackFrom);
                    break;
                default:
                    break;
            }
        }, executorService);
    }

    /**
     * Get range data.
     *
     * @param session - session
     * @param start   - first key of storage
     * @param end     - last key of storage
     */
    @Path("/v0/entities")
    public void entitiesHandler(
            @NotNull final HttpSession session,
            @NotNull final @Param(value = "start", required = true) String start,
            @NotNull final @Param(value = "end") String end) {
        if (start.isEmpty()) {
            httpHelper.sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        ((RecordStreamingSession) session).setIterator(
                session,
                new ChunkIterator(daoHelper.getRange(start, end))
        );
    }

    private void getEntityExecutor(final String id,
                                   final HttpSession session,
                                   final Request request,
                                   final AckFrom ackFrom) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            httpHelper.respond(session, daoHelper.getEntity(key));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.getEntity(key), httpClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.OK
                        || response.getStatus() == ResponseCode.NOT_FOUND)
                .collect(Collectors.toList());

        httpHelper.calculateResponse(
                session,
                notFailedResponses.size(),
                ackFrom,
                daoHelper.resolveGet(notFailedResponses));
    }

    private void deleteEntityExecutor(final String id,
                                      final HttpSession session,
                                      final Request request,
                                      final AckFrom ackFrom) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            httpHelper.respond(session, daoHelper.delEntity(key));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.delEntity(key), httpClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.ACCEPTED)
                .collect(Collectors.toList());

        httpHelper.calculateResponse(
                session,
                notFailedResponses.size(),
                ackFrom,
                new Response(Response.ACCEPTED, Response.EMPTY));
    }

    private void putEntityExecutor(final String id,
                                   final HttpSession session,
                                   final Request request,
                                   final @NotNull AckFrom ackFrom) {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            httpHelper.respond(session, daoHelper.putEntity(key, value));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.putEntity(key, value), httpClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.CREATED)
                .collect(Collectors.toList());

        httpHelper.calculateResponse(
                session,
                notFailedResponses.size(),
                ackFrom,
                new Response(Response.CREATED, Response.EMPTY));
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        log.debug("Can't understand request: {}", request);

        httpHelper.sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public HttpSession createSession(@NotNull final Socket socket) {
        return new RecordStreamingSession(socket, this, httpHelper);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("ERROR. Cant shutdown executor.", e);
            Thread.currentThread().interrupt();
        }

        for (final HttpClient client : nodeToClient.values()) {
            client.clear();
        }
    }
}
