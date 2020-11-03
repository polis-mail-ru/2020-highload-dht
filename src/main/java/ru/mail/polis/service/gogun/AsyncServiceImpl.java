package ru.mail.polis.service.gogun;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.gogun.Value;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    @NotNull
    private final DAO dao;
    private final Hashing<String> topology;
    private final ExecutorService executorService;
    @NotNull
    private final java.net.http.HttpClient client;

    /**
     * class that provides requests to lsm dao via http.
     *
     * @param port       - port
     * @param numWorkers - num of threads in executor service
     * @param queueSize  - thread queue size
     * @param dao        - key-value storage
     * @throws IOException - ioexception
     */
    public AsyncServiceImpl(final int port, final int numWorkers, final int queueSize,
                            @NotNull final DAO dao,
                            @NotNull final Hashing<String> topology) throws IOException {
        super(ServiceUtils.makeConfig(port, numWorkers));
        this.dao = dao;
        this.topology = topology;
        this.executorService = new ThreadPoolExecutor(numWorkers,
                numWorkers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error {} in {}", e, t))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());

        final Executor clientExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("Client_%d")
                        .build());
        this.client =
                java.net.http.HttpClient.newBuilder()
                        .executor(clientExecutor)
                        .connectTimeout(TIMEOUT)
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .build();
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            log.error("Can't send response {}", request, e);
        }
    }

    /**
     * provide checking api is alive.
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    private void execute(final String id, final HttpSession session,
                         final Request request) throws RejectedExecutionException {
        executorService.execute(() -> {
            try {
                handleRequest(id, request, session);
            } catch (IOException e) {
                log.error("Error sending response", e);
            }
        });
    }

    private void sendServiceUnavailable(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        } catch (IOException e) {
            log.error("Error sending response in method get", e);
        }
    }

    @NotNull
    private static HttpRequest.Builder requestForRepl(@NotNull final String node,
                                                      @NotNull final String id) {
        final String uri = node + "/v0/entity?id=" + id;
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("X-Proxy-For", "true")
                    .timeout(TIMEOUT);
        } catch (URISyntaxException e) {
            log.error("uri error", e);
            throw new IllegalStateException("uri error", e);
        }
    }

    private void handleRequest(final String id, final Request request,
                               final HttpSession session) throws IOException {
        log.debug("{} request with id: {}", request.getMethodName(), id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (request.getHeader("X-Proxy-For") != null) {
            ServiceUtils.selector(
                    () -> handlePut(key, request),
                    () -> handleGet(key),
                    () -> handleDel(key),
                    request.getMethod(),
                    session);
            return;
        }

        final ReplicasFactor replicasFactor;
        if (request.getParameter("replicas") == null) {
            replicasFactor = new ReplicasFactor(topology.all().length);
        } else {
            replicasFactor = new ReplicasFactor(request.getParameter("replicas"));
        }

        if (replicasFactor.isBad()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final List<CompletableFuture<Response>> responsesFuture = new ArrayList<>(replicasFactor.getFrom());
        final String nodeForRequest = topology.get(key);
        final List<String> replNodes = topology.getReplNodes(nodeForRequest, replicasFactor.getFrom());
        for (final String node : replNodes) {
            responsesFuture.add(proxy(node, request));
        }

        Futures.atLeastAsync(replicasFactor.getAck(), responsesFuture).whenCompleteAsync((v, t) -> {
            try {
                if (v == null) {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    return;
                }

                final MergeResponses mergeResponses = new MergeResponses(v, replicasFactor.getAck());
                v.removeIf((e) -> e.getStatus() == 500);

                ServiceUtils.selector(mergeResponses::mergePutResponses,
                        mergeResponses::mergeGetResponses,
                        mergeResponses::mergeDeleteResponses,
                        request.getMethod(),
                        session);
            } catch (IOException e) {
                log.error("error sending response", e);
            }
        }, executorService).isCancelled();
    }

    private Response handlePut(@NotNull final ByteBuffer key, @NotNull final Request request) {
        try {
            dao.upsert(key, ServiceUtils.getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);

        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response handleGet(@NotNull final ByteBuffer key) {
        final Value value;
        try {
            value = dao.getValue(key);
        } catch (IOException e) {
            log.error("Internal server error get", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        Response response;
        if (value.isTombstone()) {
            response = Response.ok(Response.EMPTY);
            response.addHeader("tombstone: " + true);
        } else {
            response = Response.ok(ServiceUtils.getArray(value.getData()));
            response.addHeader("tombstone: " + false);
        }

        response.addHeader("timestamp: " + value.getTimestamp());
        return response;
    }

    private Response handleDel(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);

        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    private CompletableFuture<Response> proxy(final String node, final Request request) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (topology.isMe(node)) {
            return ServiceUtils.selector(
                    () -> handlePut(key, request),
                    () -> handleGet(key),
                    () -> handleDel(key),
                    request.getMethod(),
                    executorService);
        }

        HttpRequest requestForReplica;
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                requestForReplica = requestForRepl(node, id)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_GET:
                requestForReplica = requestForRepl(node, id)
                        .GET()
                        .build();
                return client.sendAsync(requestForReplica, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_DELETE:
                requestForReplica = requestForRepl(node, id)
                        .DELETE()
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            default:
                return null;
        }
    }

    /**
     * Provide put/del/get requests to dao.
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    public void handleHttpPath(@Param(value = "id", required = true) @NotNull final String id,
                               final HttpSession session,
                               final Request request) {
        try {
            execute(id, session, request);
        } catch (RejectedExecutionException e) {
            sendServiceUnavailable(session);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Cant stop executor service", e);
            Thread.currentThread().interrupt();
        }
    }
}
