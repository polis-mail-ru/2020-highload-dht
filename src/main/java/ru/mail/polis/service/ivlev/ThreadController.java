package ru.mail.polis.service.ivlev;

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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Topology;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadController extends HttpServer implements Service {

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> clients;

    private static final Logger logger = LoggerFactory.getLogger(ThreadController.class);
    private static final String RESPONSE_ERROR_LOG_MESSAGE = "Fail send response: ";

    /**
     * Implementation {@link Service}.
     *
     * @param config       - config
     * @param dao          - dao
     * @param workersCount - count of thread workers
     * @param queueSize    - Blocking queue max size
     */
    public ThreadController(
            final HttpServerConfig config,
            @NotNull final DAO dao,
            final int workersCount,
            final int queueSize,
            @NotNull final Topology<String> topology) throws IOException {
        super(config, dao);
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        this.topology = topology;
        this.clients = new HashMap<>();
        for (final String node : topology.getAllNodes()) {
            logger.debug(node);
            if (topology.equalsUrl(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (clients.put(node, client) != null) {
                throw new IllegalArgumentException("Duplicate node!");
            }
            logger.debug("NODE {}, CLIENT {}", node, client.name());
        }
        executor = new ThreadPoolExecutor(
                workersCount, queueSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * End - point status.
     *
     * @param session - http session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, e);
        }
    }

    /**
     * End-point get.
     *
     * @param id      - id
     * @param session - http session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(
            @Param(value = "id", required = true) final String id,
            final HttpSession session,
            final Request request) {
        executor.execute(() -> {
            if (equalsUrlNode(id)) {
                try {
                    if (id.isEmpty()) {
                        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    } else {
                        session.sendResponse(new Response(Response.OK, toByteArray(dao.get(toByteBuffer(id)))));
                    }
                } catch (NoSuchElementException e) {
                    sendNotFound(session);
                } catch (IOException e) {
                    sendInternalServerError(session);
                }
            } else {
                sendResponseProxy(session, request, id);
            }
        });
    }

    /**
     * End-point put.
     *
     * @param id      - id
     * @param session - http session
     * @param request - http request
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(
            @Param(value = "id", required = true) final String id,
            final HttpSession session,
            final Request request) {
        executor.execute(() -> {
            if (equalsUrlNode(id)) {
                try {
                    if (id.isEmpty()) {
                        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    } else {
                        dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
                        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                    }
                } catch (IOException e) {
                    sendInternalServerError(session);
                }
            } else {
                sendResponseProxy(session, request, id);
            }
        });
    }

    /**
     * End-point delete.
     *
     * @param id      - id
     * @param session - http session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(
            @Param(value = "id", required = true) final String id,
            final HttpSession session,
            final Request request) {
        executor.execute(() -> {
            if (equalsUrlNode(id)) {
                try {
                    if (id.isEmpty()) {
                        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    } else {
                        dao.remove(toByteBuffer(id));
                        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                    }
                } catch (IOException e) {
                    sendInternalServerError(session);
                }
            } else {
                sendResponseProxy(session, request, id);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, e);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clients.values()) {
            client.close();
        }
    }

    private static ByteBuffer toByteBuffer(@NotNull final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] toByteArray(@NotNull final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }

    @NotNull
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) {
        try {
            return clients.get(node).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException e) {
            logger.error("Proxy unavailable: ", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private boolean equalsUrlNode(@NotNull final String id) {
        final String node = topology.getNodeByKey(id);
        return topology.equalsUrl(node);
    }

    private void sendResponseProxy(final HttpSession session, final Request request, final String id) {
        try {
            final String node = topology.getNodeByKey(id);
            session.sendResponse(proxy(node, request));
        } catch (IOException ioException) {
            logger.error(RESPONSE_ERROR_LOG_MESSAGE, ioException);
        }
    }

    private void sendInternalServerError(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error(RESPONSE_ERROR_LOG_MESSAGE, ioException);
        }
    }

    private void sendNotFound(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error(RESPONSE_ERROR_LOG_MESSAGE, ioException);
        }
    }
}
