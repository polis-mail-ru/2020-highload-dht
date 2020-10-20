package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class AsyncService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private static final String UNAVAILABLE_MESSAGE = "Can't send unavailable error";
    private static final String SCHEDULE_MESSAGE = "Can't schedule request for execution";
    private static final String ERROR_SEND_MESSAGE = "Error in sending error";
    private final DAO dao;
    private final ExecutorService es;
    private final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;

    private AsyncService(final int port, @NotNull final DAO dao,
                         final int workers, final int queueSize,
                         @NotNull final ShardingPolicy<ByteBuffer, String> policy) throws IOException {
        super(configFrom(port));
        assert 0 < workers;
        assert 0 < queueSize;
        this.policy = policy;
        this.urlToClient = urltoClientFromSet(this.policy.all());
        this.dao = dao;
        this.es = new ThreadPoolExecutor(
                workers,
                workers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("worker-%d").setUncaughtExceptionHandler((t, e) ->
                        logger.error("Error in {} when processing request", t, e)).build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private Map<String, HttpClient> urltoClientFromSet(String[] nodes) {
        Map<String, HttpClient> result = new HashMap<>();
        for (var url : nodes) {
            if (url.equals(this.policy.homeNode())) {
                continue;
            }
            if (result.put(url, new HttpClient(new ConnectionString(url))) != null) {
                throw new RuntimeException("Duplicated url in nodes.");
            }
        }
        return result;
    }

    private void chooseNode(@NotNull final String id, @NotNull final Request request,
                            @NotNull final Runnable homeRunnableFunction) {
        final var node = this.policy.getNode(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
        if (node.equals(this.policy.homeNode())) {
            homeRunnableFunction.run();
        } else {
            proxy(node, request);
        }
    }

    private void proxy(@NotNull final String url, @NotNull final Request request) {
        try {
            request.addHeader("X-Proxy-For: " + url);
            urlToClient.get(url).invoke(request);
        } catch (Exception e) {
            logger.error("Error in proxy with dest {}", url, e);
        }
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
    public static AsyncService of(final int port, @NotNull final DAO dao,
                                  final int workers, final int queueSize,
                                  @NotNull final ShardingPolicy<ByteBuffer, String> policy) throws IOException {
        return new AsyncService(port, dao, workers, queueSize, policy);
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

    public void get(@NotNull final String id, @NotNull final HttpSession session) {
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Response response;
        response = handlingGetSendingResponse(id, session, buffer);
        if (response == null) return;
        sendUserResponse(session, response, ERROR_SEND_MESSAGE);
    }

    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleGet(@Param(value = "id", required = true) final String id,
                          @NotNull final Request request,
                          @NotNull final HttpSession session) {
        if (id.isEmpty()) {
            logger.error("Empty key in getting");
            sendResponse(session, Response.BAD_REQUEST);
            return;
        }
        chooseNode(id, request, () -> asyncExecuteThreadPool(() -> get(id, session), session));
    }

    @Nullable
    private Response handlingGetSendingResponse(@Param(value = "id", required = true) final String id,
                                                @NotNull final HttpSession session,
                                                @NotNull final ByteBuffer buffer) {
        Response response;
        try {
            final ByteBuffer value = dao.get(buffer);
            session.sendResponse(Response.ok(fromByteBuffer(value)));
            return null;
        } catch (NoSuchElementException e) {
            logger.error("No such element key(size: {}) in dao", id.length());
            response = new Response(Response.NOT_FOUND, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in getting key(size: {}) from dao", id.length());
            response = new Response(Response.INTERNAL_ERROR, EMPTY);
        }
        return response;
    }

    public void upsert(@NotNull final String id, @NotNull final Request request, @NotNull final HttpSession session) {
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        handlingUpsertSendingRequest(id, request, session, buffer);
    }


    /**
     * Basic implementation of http put handling.
     *
     * @param id      key
     * @param request value for putting in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void handlePut(@Param(value = "id", required = true) final String id,
                          @NotNull final Request request,
                          @NotNull final HttpSession session) {
        if (id.isEmpty()) {
            logger.error("Empty key in putting");
            sendResponse(session, Response.BAD_REQUEST);
        }
        chooseNode(id, request, () -> asyncExecuteThreadPool(() -> upsert(id, request, session), session));
    }

    private void handlingUpsertSendingRequest(@Param(value = "id", required = true) final String id,
                                              @NotNull final Request request,
                                              @NotNull final HttpSession session,
                                              @NotNull final ByteBuffer buffer) {
        try {
            dao.upsert(buffer, ByteBuffer.wrap(request.getBody()));
            session.sendResponse(new Response(Response.CREATED, EMPTY));
        } catch (IOException e) {
            logger.error("IOException in putting key(size: {}) from dao", id.length());
            sendResponse(session, Response.INTERNAL_ERROR);
        }
    }

    public void delete(@NotNull final String id, @NotNull final HttpSession session) {
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        handlingRemoveResponseSending(id, session, buffer);
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void handleDelete(@Param(value = "id", required = true) final String id,
                             @NotNull final Request request,
                             @NotNull final HttpSession session) {
        if (id.isEmpty()) {
            logger.error("Empty key in deleting");
            sendResponse(session, Response.BAD_REQUEST);
            return;
        }

        chooseNode(id, request, () -> asyncExecuteThreadPool(() -> delete(id, session), session));
    }

    public void asyncExecuteThreadPool(@NotNull final Runnable runnable, @NotNull final HttpSession session) {
        try {
            runnable.run();
        } catch (RejectedExecutionException e) {
            logger.error(SCHEDULE_MESSAGE, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(UNAVAILABLE_MESSAGE, ioException);
            }
        }
    }

    private void handlingRemoveResponseSending(@Param(value = "id", required = true) final String id,
                                               final HttpSession session, final ByteBuffer buffer) {
        try {
            dao.remove(buffer);
            session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
        } catch (IOException e) {
            logger.error("IOException in removing key(size: {}) from dao", id.length());
            sendResponse(session, Response.INTERNAL_ERROR);
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
            this.es.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown executor", e);
            Thread.currentThread().interrupt();
        }


        for (final HttpClient client : this.urlToClient.values()) {
            client.clear();
        }
    }
}
