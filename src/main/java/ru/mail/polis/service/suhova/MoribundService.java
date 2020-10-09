package ru.mail.polis.service.suhova;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MoribundService extends HttpServer implements Service {

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    private static final Logger logger = LoggerFactory.getLogger(MoribundService.class);

    /**
     * Implementation {@link Service}.
     *
     * @param port         - port
     * @param dao          - dao
     * @param workersCount - count of executor workers
     * @param queueSize    - ArrayBlockingQueue max size
     */
    public MoribundService(final int port,
                           @NotNull final DAO dao,
                           final int workersCount,
                           final int queueSize) throws IOException {
        super(getConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        executor = new ThreadPoolExecutor(
            workersCount, queueSize,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                .setNameFormat("worker_%d")
                .build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Request to get a value by id.
     * Path /v0/entity
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id, final HttpSession session) {
        try {
            if (id.isEmpty()) {
                logger.warn("FAIL GET! Id is empty!");
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            }
            session.sendResponse(new Response(Response.OK, toByteArray(dao.get(toByteBuffer(id)))));
        } catch (NoSuchElementException e) {
            try {
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException ioException) {
                logger.error("FAIL GET! Can't send response.", ioException);
            }
        } catch (IOException e) {
            logger.error("FAIL GET! id: {}, error: {}", id, e.getMessage());
            try {
                session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            } catch (IOException ioException) {
                logger.error("FAIL GET! Can't send response.", ioException);
            }
        }
    }

    /**
     * Request to put a value by id.
     * Path /v0/entity
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(value = "id", required = true) final String id,
                    final HttpSession session,
                    final Request request) {
        executor.execute(() -> {
            try {
                if (id.isEmpty()) {
                    logger.warn("FAIL PUT! Id is empty!");
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                logger.error("FAIL PUT! id: {}, request: {}, error: {}", id, request.getBody(), e.getMessage());
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("FAIL PUT! Can't send response.", ioException);
                }
            }
        });
    }

    /**
     * Request to delete a value by id.
     * Path /v0/entity
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) final String id, final HttpSession session) {
        executor.execute(() -> {
            try {
                if (id.isEmpty()) {
                    logger.warn("FAIL DELETE! Id is empty!");
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                dao.remove(toByteBuffer(id));
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                logger.error("FAIL DELETE! id: {}, error: {}", id, e.getMessage());
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("FAIL DELETE! Can't send response.", ioException);
                }
            }
        });
    }

    /**
     * All requests to /v0/status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        executor.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                logger.error("FAIL STATUS! Can't send response.", e);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        executor.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Can't send response.", e);
            }
        });
    }

    private static HttpServerConfig getConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        acceptorConfig.deferAccept = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
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

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown execution");
            Thread.currentThread().interrupt();
        }
    }
}
