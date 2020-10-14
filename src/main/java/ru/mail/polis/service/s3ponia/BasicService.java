package ru.mail.polis.service.s3ponia;

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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class BasicService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(BasicService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private static final String UNAVAILABLE_MESSAGE = "Can't send unavailable error";
    private static final String SCHEDULE_MESSAGE = "Can't schedule request for execution";
    private static final String ERROR_SEND_MESSAGE = "Error in sending error";
    private final DAO dao;
    private final ExecutorService es;

    private BasicService(final int port, @NotNull final DAO dao,
                         final int workers, final int queueSize) throws IOException {
        super(configFrom(port));
        assert 0 < workers;
        assert 0 < queueSize;
        this.dao = dao;
        this.es = new ThreadPoolExecutor(workers, workers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) ->
                                logger.error("Error in {} when processing request", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[1];
        config.acceptors[0] = ac;
        return config;
    }

    @NotNull
    private static byte[] fromByteBuffer(@NotNull final ByteBuffer b) {
        final byte[] out = new byte[b.remaining()];
        b.get(out);
        return out;
    }

    @NotNull
    public static BasicService of(final int port, @NotNull final DAO dao,
                                  final int workers, final int queueSize) throws IOException {
        return new BasicService(port, dao, workers, queueSize);
    }

    /**
     * Handling status request.
     *
     * @param session current Session
     */
    @Path("/v0/status")
    public void handleStatus(final HttpSession session) {
        try {
            this.es.execute(() -> {
                sendUserResponse(session, Response.ok("OK"), "Error in sending status");
            });
        } catch (RejectedExecutionException e) {
            logger.error(SCHEDULE_MESSAGE, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(UNAVAILABLE_MESSAGE, ioException);
            }
        }
    }

    private void sendUserResponse(final HttpSession session, final Response ok, final String s) {
        try {
            session.sendResponse(ok);
        } catch (IOException e) {
            logger.error(s, e);
        }
    }

    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleGet(@Param(value = "id", required = true) final String id, final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in getting");
                    sendResponse(session, Response.BAD_REQUEST);
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                final Response response;
                try {
                    final ByteBuffer value = dao.get(buffer);
                    session.sendResponse(Response.ok(fromByteBuffer(value)));
                    return;
                } catch (NoSuchElementException e) {
                    logger.error("No such element key(size: {}) in dao", id.length());
                    response = new Response(Response.NOT_FOUND, EMPTY);
                } catch (IOException e) {
                    logger.error("IOException in getting key(size: {}) from dao", id.length());
                    response = new Response(Response.INTERNAL_ERROR, EMPTY);
                }
                sendUserResponse(session, response, ERROR_SEND_MESSAGE);
            });
        } catch (RejectedExecutionException e) {
            logger.error(SCHEDULE_MESSAGE, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(UNAVAILABLE_MESSAGE, ioException);
            }
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id      key
     * @param request value for putting in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void handlePut(@Param(value = "id", required = true) final String id, final Request request,
                          final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in putting");
                    sendResponse(session, Response.BAD_REQUEST);
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                try {
                    dao.upsert(buffer, ByteBuffer.wrap(request.getBody()));
                    session.sendResponse(new Response(Response.CREATED, EMPTY));
                } catch (IOException e) {
                    logger.error("IOException in putting key(size: {}) from dao", id.length());
                    sendResponse(session, Response.INTERNAL_ERROR);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.error(SCHEDULE_MESSAGE, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(UNAVAILABLE_MESSAGE, ioException);
            }
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void handleDelete(@Param(value = "id", required = true) final String id, final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in deleting");
                    sendResponse(session, Response.BAD_REQUEST);
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                try {
                    dao.remove(buffer);
                    session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                } catch (IOException e) {
                    logger.error("IOException in removing key(size: {}) from dao", id.length());
                    sendResponse(session, Response.INTERNAL_ERROR);
                }

            });
        } catch (RejectedExecutionException e) {
            logger.error(SCHEDULE_MESSAGE, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(UNAVAILABLE_MESSAGE, ioException);
            }
        }
    }

    private void sendResponse(final HttpSession session, final String internalError) {
        try {
            session.sendResponse(new Response(internalError, EMPTY));
        } catch (IOException ioException) {
            logger.error(ERROR_SEND_MESSAGE, ioException);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        this.es.shutdown();
        try {
            this.es.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown executor", e);
            Thread.currentThread().interrupt();
        }
    }
}
