package ru.mail.polis.service.s3ponia;

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

    private Map<String, HttpClient> urltoClientFromSet(@NotNull final String... nodes) {
        final Map<String, HttpClient> result = new HashMap<>();
        for (final var url : nodes) {
            if (url.equals(this.policy.homeNode())) {
                continue;
            }
            if (result.put(url, new HttpClient(new ConnectionString(url))) != null) {
                throw new RuntimeException("Duplicated url in nodes.");
            }
        }
        return result;
    }

    private void proxy(
            @NotNull final String node,
            @NotNull final HttpSession session,
            @NotNull final Request request) throws IOException {
        try {
            request.addHeader("X-Proxy-For: " + node);
            session.sendResponse(this.urlToClient.get(node).invoke(request));
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            logger.error("Can't proxy request", exception);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
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
    public void status(final HttpSession session) throws IOException {
        try {
            this.es.execute(() -> handlingStatusError(session));
        } catch (RejectedExecutionException e) {
            logger.error("Internal error in status handling", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }

    private void handlingStatusError(@NotNull final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            logger.error("Error in sending status", e);
        }
    }

    /**
     * Process getting from dao.
     *
     * @param key     key for getting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void get(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
        try {
            session.sendResponse(Response.ok(fromByteBuffer(dao.get(key))));
        } catch (NoSuchElementException noSuchElementException) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in getting key(size: {}) from dao", key.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id,
                    @NotNull final Request request,
                    @NotNull final HttpSession session) throws IOException {
        if (validateId(id, session, "Empty key in getting")) return;

        chooseNode(urlFromKey(byteBufferFromString(id)), session,
                request,
                () -> {
                    try {
                        get(byteBufferFromString(id), session);
                    } catch (IOException e) {
                        logger.error("Error in sending get request", e);
                    }
                });
    }

    private boolean validateId(@NotNull final String id,
                               @NotNull final HttpSession session,
                               @NotNull final String s) throws IOException {
        if (id.isEmpty()) {
            logger.error(s);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return true;
        }
        return false;
    }

    /**
     * Process upserting to dao.
     *
     * @param key     key for upserting
     * @param value   value for upserting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       @NotNull final HttpSession session) throws IOException {
        try {
            dao.upsert(key, value);
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in putting key(size: {}), value(size: {}) from dao",
                    key.capacity(), value.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private ByteBuffer byteBufferFromString(@NotNull final String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private String urlFromKey(@NotNull final ByteBuffer key) {
        return this.policy.getNode(key);
    }

    private void chooseNode(
            @NotNull final String node,
            @NotNull final HttpSession httpSession,
            @NotNull final Request request,
            @NotNull final Runnable runFunction) throws IOException {
        if (this.policy.homeNode().equals(node)) {
            this.es.execute(runFunction);
        } else {
            proxy(node, httpSession, request);
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
    public void put(@Param(value = "id", required = true) final String id,
                    @NotNull final Request request,
                    @NotNull final HttpSession session) throws IOException {
        if (validateId(id, session, "Empty key in deleting")) return;

        chooseNode(urlFromKey(byteBufferFromString(id)), session,
                request,
                () -> {
                    try {
                        upsert(byteBufferFromString(id),
                                ByteBuffer.wrap(request.getBody()), session);
                    } catch (IOException e) {
                        logger.error("Error in sending put request", e);
                    }
                });
    }

    /**
     * Process deleting from dao.
     *
     * @param key     record's key to delete
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void delete(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
        try {
            dao.remove(key);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in removing key(size: {}) from dao", key.capacity());
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) final String id,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (validateId(id, session, "Empty key in deleting")) return;

        chooseNode(urlFromKey(byteBufferFromString(id)), session, request,
                () -> {
                    try {
                        delete(byteBufferFromString(id), session);
                    } catch (IOException e) {
                        logger.error("Error in sending delete request", e);
                    }
                });
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
