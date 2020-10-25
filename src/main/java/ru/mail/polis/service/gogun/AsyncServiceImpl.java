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
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public AsyncServiceImpl(final int port,
                            final int numWorkers,
                            final int queueSize,
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
            if (topology.isMe(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node));
            if (nodeClients.containsKey(node)) {
                continue;
            }

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
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            log.error("Error sending response", e);
        }
    }

    private void execute(final String id,
                         final HttpSession session,
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

    private void handleRequest(final String id,
                               final Request request,
                               final HttpSession session) throws IOException {
        log.debug("PUT request with id: {}", id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer key = getBuffer(id.getBytes(UTF_8));
        final String node = topology.get(key);

        if (!topology.isMe(node)) {
            proxy(node, request, session);
            return;
        }
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                try {
                    dao.upsert(key, getBuffer(request.getBody()));
                } catch (IOException e) {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    log.error("Internal server error put", e);
                    return;
                } catch (NoSuchElementException e) {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    return;
                }

                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                break;
            case Request.METHOD_GET:
                final ByteBuffer buffer;
                try {
                    buffer = dao.get(key);
                } catch (IOException e) {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    log.error("Internal server error get", e);
                    return;
                } catch (NoSuchElementException e) {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    return;
                }

                session.sendResponse(Response.ok(getArray(buffer)));
                break;
            case Request.METHOD_DELETE:
                try {
                    dao.remove(key);
                } catch (IOException e) {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    log.error("Internal server error del", e);
                    return;
                } catch (NoSuchElementException e) {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    return;
                }

                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                break;
            default:
                break;
        }

    }

    private void proxy(final String node, final Request request, final HttpSession session) throws IOException {
        try {
            request.addHeader("X-Proxy-For: " + node);
            session.sendResponse(nodeClients.get(node).invoke(request));
        } catch (IOException | InterruptedException | PoolException | HttpException e) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Provide getting data by id from lsm.
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) @NotNull final String id,
                    final HttpSession session,
                    final Request request) {
        try {
            execute(id, session, request);
        } catch (RejectedExecutionException e) {
            sendServiceUnavailable(session);
        }
    }

    /**
     * Provide putting/updating data by key.
     *
     * @param id      - key
     * @param request - value
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void upsert(@Param(value = "id", required = true) @NotNull final String id,
                       final Request request,
                       final HttpSession session) {
        try {
            execute(id, session, request);
        } catch (RejectedExecutionException e) {
            sendServiceUnavailable(session);
        }
    }

    /**
     * Provide deleting by id.
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) @NotNull final String id,
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
