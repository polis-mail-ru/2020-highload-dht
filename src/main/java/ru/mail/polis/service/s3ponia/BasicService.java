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
import java.util.concurrent.*;

public final class BasicService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(BasicService.class);
    private static final byte[] EMPTY = Response.EMPTY;
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
    public static BasicService of(final int port, @NotNull final DAO dao, final int workers, final int queueSize) throws IOException {
        return new BasicService(port, dao, workers, queueSize);
    }

    @Path("/v0/status")
    public void handleStatus(final HttpSession session) throws IOException {
        try {
            this.es.execute(() -> {
                try {
                    session.sendResponse(Response.ok("OK"));
                    Thread.sleep(10);
                } catch (Exception e) {
                    logger.error("Error in sending status", e);
                }
            });
        } catch (Exception e) {
            logger.error("Can't schedule request for execution", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error("Can't send internal error", ioException);
            }
        }
    }

    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     * @return response with value from database by key or http error code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleGet(@Param(value = "id", required = true) final String id, final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in getting");
                    try {
                        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
                    } catch (IOException e) {
                        logger.error("Error in sending error", e);
                    }
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                try {
                    final ByteBuffer value = dao.get(buffer);
                    try {
                        session.sendResponse(Response.ok(fromByteBuffer(value)));
                    } catch (IOException ioException) {
                        logger.error("Error in sending response", ioException);
                    }
                } catch (NoSuchElementException e) {
                    logger.error("No such element key(size: {}) in dao", id.length());
                    try {
                        session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending error", ioException);
                    }
                } catch (IOException e) {
                    logger.error("IOException in getting key(size: {}) from dao", id.length());
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending error", ioException);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Can't schedule request for execution", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error("Can't send internal error", ioException);
            }
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id      key
     * @param request value for putting in database
     * @return response with http code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response handlePut(@Param(value = "id", required = true) final String id, final Request request,
                              final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in putting");
                    try {
                        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
                    } catch (IOException e) {
                        logger.error("Error in sending error", e);
                    }
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                try {
                    dao.upsert(buffer, ByteBuffer.wrap(request.getBody()));
                    try {
                        session.sendResponse(new Response(Response.CREATED, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending response", ioException);
                    }
                } catch (IOException e) {
                    logger.error("IOException in putting key(size: {}) from dao", id.length());
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending error", ioException);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Can't schedule request for execution", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error("Can't send internal error", ioException);
            }
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     * @return response with http code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response handleDelete(@Param(value = "id", required = true) final String id, final HttpSession session) {
        try {
            this.es.execute(() -> {
                if (id.isEmpty()) {
                    logger.error("Empty key in deleting");
                    try {
                        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
                    } catch (IOException e) {
                        logger.error("Error in sending error", e);
                    }
                }

                final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                try {
                    dao.remove(buffer);
                    try {
                        session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending response", ioException);
                    }
                } catch (IOException e) {
                    logger.error("IOException in removing key(size: {}) from dao", id.length());
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error in sending error", ioException);
                    }
                }

            });
        } catch (Exception e) {
            logger.error("Can't schedule request for execution", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error("Can't send internal error", ioException);
            }
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
        }
    }
}
