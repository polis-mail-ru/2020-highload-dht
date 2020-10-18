package ru.mail.polis.service.re1nex;

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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncService extends HttpServer implements Service {
    @NonNull
    private static final String RESPONSE_ERROR = "Can't send response error";
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;

    /**
     * Service for concurrent work with requests.
     *
     * @param port         - Server port
     * @param dao          - DAO impl
     * @param workersCount - number workers in pool
     * @param queueSize    - size of task's queue
     */
    public AsyncService(final int port,
                        @NotNull final DAO dao,
                        final int workersCount,
                        final int queueSize) throws IOException {
        super(provideConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        this.executor = new ThreadPoolExecutor(
                workersCount,
                workersCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Error {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    /**
     * Provide service status.
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.info("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Provide request to get the value by id.
     * send 200 OK ||  400 / 404 / 500 ERROR
     *
     * @param id      - key
     * @param session - current HttpSession
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id, final HttpSession session) {
        executeTask(() -> {
                    try {
                        if (id.isEmpty()) {
                            logger.info("GET failed! Id is empty!");
                            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                            return;
                        }
                        final ByteBuffer result = dao.get(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
                        if (result.hasRemaining()) {
                            final byte[] resultByteArray = new byte[result.remaining()];
                            result.get(resultByteArray);
                            session.sendResponse(new Response(Response.OK, resultByteArray));
                        } else {
                            session.sendResponse(new Response(Response.OK, Response.EMPTY));
                        }
                    } catch (IOException e) {
                        logger.error("GET element " + id, e);
                        sendErrorResponse(session, Response.INTERNAL_ERROR);
                    }
                },
                session);
    }

    /**
     * Provide request to put the value by id.
     * send 201 Created || 400 / 500 ERROR
     *
     * @param id      - key
     * @param request - Request with value
     * @param session - current HttpSession
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(value = "id", required = true) final String id,
                    @NotNull final Request request,
                    final HttpSession session) {
        executeTask(() -> {
                    try {
                        if (id.isEmpty()) {
                            logger.info("PUT failed! Id is empty!");
                            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                            return;
                        }
                        dao.upsert(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)),
                                ByteBuffer.wrap(request.getBody()));
                        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                    } catch (IOException e) {
                        logger.error("PUT failed! Cannot put the element: {}. Request: {}. Cause: {}",
                                id, request.getBody(), e.getCause());
                        sendErrorResponse(session, Response.INTERNAL_ERROR);
                    }
                },
                session);
    }

    private void sendErrorResponse(final HttpSession session, final String internalError) {
        try {
            session.sendResponse(new Response(internalError, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error(RESPONSE_ERROR, ioException);
        }
    }

    /**
     * Provide request to delete the value by id.
     * send 202 Accepted ||  400 / 500 ERROR
     *
     * @param id      - key
     * @param session - current HttpSession
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) final String id, final HttpSession session) {
        executeTask(() -> {
                    try {
                        if (id.isEmpty()) {
                            logger.info("DELETE failed! Id is empty!");
                            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                            return;
                        }
                        dao.remove(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
                        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                    } catch (IOException e) {
                        logger.error("DELETE failed! Cannot get the element {}.\n Error: {}", id, e.getMessage(), e);
                        sendErrorResponse(session, Response.INTERNAL_ERROR);
                    }
                },
                session);
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

    private void executeTask(final Runnable task, final HttpSession session) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            logger.error("Execute failed! Error: {}", e);
            sendErrorResponse(session, Response.SERVICE_UNAVAILABLE);
        }
    }
}
