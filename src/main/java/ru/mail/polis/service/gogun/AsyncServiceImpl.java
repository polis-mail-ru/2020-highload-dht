package ru.mail.polis.service.gogun;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.gogun.Value;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);

    @NotNull
    private final DAO dao;
    private final Hashing<String> topology;
    private final ExecutorService executorService;
    private final Map<String, HttpClient> nodeClients;

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
        super(makeConfig(port, numWorkers));
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
        this.nodeClients = new HashMap<>();
        for (final String node : topology.all()) {
            final HttpClient client = new HttpClient(new ConnectionString(node));
            nodeClients.put(node, client);
        }
    }

    @NotNull
    private static HttpServerConfig makeConfig(final int port, final int numWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numWorkers;

        return config;
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

    private ByteBuffer getBuffer(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    private byte[] getArray(final ByteBuffer buffer) {
        byte[] body;
        if (buffer.hasRemaining()) {
            body = new byte[buffer.remaining()];
            buffer.get(body);
        } else {
            body = Response.EMPTY;
        }

        return body;
    }

    private void sendServiceUnavailable(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        } catch (IOException e) {
            log.error("Error sending response in method get", e);
        }
    }

    private void handleRequest(final String id, final Request request,
                               final HttpSession session) throws IOException {
        log.debug("PUT request with id: {}", id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer key = getBuffer(id.getBytes(UTF_8));

        if (request.getHeader("X-Proxy-For: ") != null) {
            selector(() -> handlePut(key, request),
                    () -> handleGet(key),
                    () -> handleGet(key),
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

        final List<Response> responses = new ArrayList<>();
        final String nodeForRequest = topology.get(key);
        final MergeResponses mergeResponses = new MergeResponses();

        final List<String> replNodes = topology.getReplNodes(nodeForRequest, replicasFactor.getFrom());
        for (final String node : replNodes) {
            responses.add(proxy(node, request));
        }

        responses.removeIf((e) -> e.getStatus() == 500);
        selector(() -> mergeResponses.mergePutResponses(responses, replicasFactor.getAck()),
                () -> mergeResponses.mergeGetResponses(responses, replicasFactor.getAck()),
                () -> mergeResponses.mergeDeleteResponses(responses, replicasFactor.getAck()),
                request.getMethod(),
                session);

    }

    private void selector(final Supplier<Response> putRequest,
                          final Supplier<Response> getRequest,
                          final Supplier<Response> deleteRequest,
                          final int method,
                          final HttpSession session) throws IOException {
        switch (method) {
            case Request.METHOD_PUT:
                session.sendResponse(putRequest.get());
                break;
            case Request.METHOD_GET:
                session.sendResponse(getRequest.get());
                break;
            case Request.METHOD_DELETE:
                session.sendResponse(deleteRequest.get());
                break;
            default:
                break;
        }
    }

    private Response handlePut(@NotNull final ByteBuffer key, @NotNull final Request request) {
        try {
            dao.upsert(key, getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);

        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
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
            response = Response.ok(getArray(value.getData()));
        }

        response.addHeader("timestamp: " + value.getTimestamp());
        response.addHeader("tombstone: " + false);
        return response;
    }

    private Response handleDel(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);

        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);

        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    private Response proxy(final String node, final Request request) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = getBuffer(id.getBytes(UTF_8));
        try {
            if (topology.isMe(node)) {
                switch (request.getMethod()) {
                    case Request.METHOD_PUT:
                        return handlePut(key, request);
                    case Request.METHOD_GET:
                        return handleGet(key);
                    case Request.METHOD_DELETE:
                        return handleDel(key);
                    default:
                        break;
                }
            }
            request.addHeader("X-Proxy-For: " + node);
            return nodeClients.get(node).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
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
                               @Param(value = "replicas") @NotNull final String replicas,
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
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Cant stop executor service", e);
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : nodeClients.values()) {
            client.clear();
        }
    }
}
