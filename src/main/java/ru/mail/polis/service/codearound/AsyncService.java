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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncService extends HttpServer implements Service {

    private final DAO dao;
    private final ExecutorService exec;
    Logger logger = LoggerFactory.getLogger(TaskService.class);
    final static String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved\n";
    final static String IO_ERROR_LOG = "IO exception raised\n";

    /**
     * async service impl const.
     * @param port local server listening port
     * @param dao DAO instance
     * @param workerPoolSize selector pool size
     */
    public AsyncService(final int port, @NotNull final DAO dao,
                        final int workerPoolSize, final int queueSize) throws IOException {

        super(TaskServerConfig.getConfig(port));
        assert workerPoolSize > 0;
        assert queueSize > 0;

        this.dao = dao;
        this.exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Worker {} fails running: {}\n", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

     /**
     * fires formation request to make sure server is alive.
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
        exec.execute(() -> {
            try {
                session.sendResponse(Response.ok("Server is running...\n"));
            } catch (IOException exc) {
                logger.error("Sending request fails, raising exception\n", exc);
            }
        });
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
            session.sendError(Response.BAD_REQUEST,"Identifier is required as parameter. Error handling request\n");
        }

        final ByteBuffer buf = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        switch (req.getMethod()) {
            case Request.METHOD_GET:
                get(buf, session);
                break;
            case Request.METHOD_PUT:
                upsert(buf, req, session);
                break;
            case Request.METHOD_DELETE:
                delete(buf, session);
                break;
            default:
                throw new NoSuchMethodException("No handler is available for request method. "
                        + "Failed determining response\n");
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
                logger.error(NOT_FOUND_ERROR_LOG);
            } catch (IOException exc) {
                logger.error(IO_ERROR_LOG, exc);
            }
        });
    }

    /**
     * PUT request async handler.
     * @param key - key to either add new record or to modify an existing one
     * @param req client request
     * @param session ongoing session instance
     */
    private void upsert(@NotNull final ByteBuffer key, @NotNull final Request req, @NotNull final HttpSession session) {

        final ByteBuffer val = ByteBuffer.wrap(req.getBody());

        exec.execute(() -> {
            try {
                upsertAsync(key, session, val);
            } catch (IOException exc) {
                logger.error(IO_ERROR_LOG, exc);
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
            } catch (NoSuchElementException exc) {
                logger.error(NOT_FOUND_ERROR_LOG);
            } catch (IOException exc) {
                logger.error(IO_ERROR_LOG, exc);
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

        ByteBuffer buf = null;

        try {
            buf = dao.get(key);
        } catch (NoSuchElementException exc) {
            logger.error(NOT_FOUND_ERROR_LOG);
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }

        assert buf != null;
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
            logger.error(IO_ERROR_LOG, exc);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
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
        } catch (NoSuchElementException exc) {
            logger.error(NOT_FOUND_ERROR_LOG);
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException exc) {
            logger.error(IO_ERROR_LOG, exc);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
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
            logger.error("Failed executor termination\n");
            Thread.currentThread().interrupt();
        }
    }
}
