package ru.mail.polis.service.codearound;

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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncService extends HttpServer implements Service {

    private final DAO dao;
    private final ExecutorService exec;
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);
    private static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    private static final String IO_ERROR_LOG = "IO exception raised";
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    private static final String GET_RESPONSE_ERROR_LOG = "Error sending GET response";
    private static final String PUT_RESPONSE_ERROR_LOG = "Error sending PUT response";
    private static final String DELETE_RESPONSE_ERROR_LOG = "Error sending DELETE response";

    /**
     * async service impl const.
     * @param port local server listening port
     * @param dao DAO instance
     * @param workerPoolSize selector pool size
     */
    public AsyncService(final int port,
                        @NotNull final DAO dao,
                        final int workerPoolSize,
                        final int queueSize) throws IOException {

        super(TaskServerConfig.getConfig(port));
        assert workerPoolSize > 0;
        assert queueSize > 0;

        this.dao = dao;
        this.exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * fires formation request to make sure server is alive.
     * @param session ongoing session instance
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
           session.sendResponse(Response.ok("Server is running..."));
    }

    /**
     * returns server status, request-specific response as well.
     * @param id String object to be processed as a key in terms of data storage design
     * @param req client request
     * @param session ongoing session instance
     */
    @Path("/v0/entity")
    public void entity(
            @Param(value = "id", required = true) final String id,
            @NotNull final Request req,
            @NotNull final HttpSession session) throws NoSuchMethodException, IOException {

        if (id.isEmpty()) {
            session.sendError(Response.BAD_REQUEST,"Identifier is required as parameter. Error handling request");
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        switch (req.getMethod()) {
            case Request.METHOD_GET:
                get(buf, session);
                break;
            case Request.METHOD_PUT:
                upsert(buf, req.getBody(), session);
                break;
            case Request.METHOD_DELETE:
                delete(buf, session);
                break;
            default:
                throw new NoSuchMethodException("No handler is available for request method. "
                        + "Failed determining response");
        }
    }

    /**
     * GET request async handler.
     * @param key - key searched
     * @param session ongoing session instance
     */
    private void get(@NotNull final ByteBuffer key, @NotNull final HttpSession session) {

        exec.execute(() -> {
            try {
                getAsync(key, session);
            } catch (NoSuchElementException exc) {
                LOGGER.info(NOT_FOUND_ERROR_LOG);
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException exc1) {
                    LOGGER.error(GET_RESPONSE_ERROR_LOG);
                }
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException exc1) {
                    LOGGER.error(GET_RESPONSE_ERROR_LOG);
                }
            } catch (IOException exc1) {
                LOGGER.error(IO_ERROR_LOG, exc1);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException exc2) {
                    LOGGER.error(GET_RESPONSE_ERROR_LOG);
                }
            }
        });
    }

    /**
     * PUT request async handler.
     * @param key - key to either add new record or to modify an existing one
     * @param byteVal key-associate value
     * @param session ongoing session instance
     */
    private void upsert(@NotNull final ByteBuffer key,
                        @NotNull final byte[] byteVal,
                        @NotNull final HttpSession session) {

        final ByteBuffer val = ByteBuffer.wrap(byteVal);

        exec.execute(() -> {
            try {
                upsertAsync(key, session, val);
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException exc1) {
                    LOGGER.error(PUT_RESPONSE_ERROR_LOG);
                }
            } catch (IOException exc1) {
                LOGGER.error(IO_ERROR_LOG, exc1);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException exc2) {
                    LOGGER.error(PUT_RESPONSE_ERROR_LOG);
                }
            }
        });
    }

    /**
     * DELETE request async handler.
     * @param key - key searched
     * @param session ongoing session instance
     */
    private void delete(@NotNull final ByteBuffer key, @NotNull final HttpSession session) {

        exec.execute(() -> {
            try {
                removeAsync(key, session);
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException exc1) {
                    LOGGER.error(DELETE_RESPONSE_ERROR_LOG);
                }
            } catch (IOException exc1) {
                LOGGER.error(IO_ERROR_LOG, exc1);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException exc2) {
                    LOGGER.error(DELETE_RESPONSE_ERROR_LOG);
                }
            }
        });
    }

    /**
     * DAO-resolved async GET handler impl.
     * @param key - target key
     * @param session ongoing session instance
     *
     */
    private void getAsync(@NotNull final ByteBuffer key,
                          @NotNull final HttpSession session) throws IOException {

        ByteBuffer buf = dao.get(key);
        session.sendResponse(Response.ok(DAOByteOnlyConverter.readByteArray(buf)));
    }

    /**
     * DAO-resolved async PUT handler impl.
     * @param key - target key
     * @param session ongoing session instance
     * @param val - key-specific value to return by server response
     */
    private void upsertAsync(@NotNull final ByteBuffer key,
                             @NotNull final HttpSession session,
                             final ByteBuffer val) throws IOException {

        try {
            dao.upsert(key, val);
        } catch (IOException exc) {
            LOGGER.error(IO_ERROR_LOG, exc);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }
        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    /**
     * DAO-resolved async DELETE handler impl.
     * @param key - target key
     * @param session ongoing session instance
     */
    private void removeAsync(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {

        try {
            dao.remove(key);
        } catch (IOException exc) {
            LOGGER.error(IO_ERROR_LOG, exc);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }
        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
    }

    /**
     * default handler.
     * @param req client host request
     * @param session ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * terminates ExecutorService process.
     */
    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Failed executor termination");
            Thread.currentThread().interrupt();
        }
    }
}
