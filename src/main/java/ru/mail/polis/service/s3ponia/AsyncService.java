package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.Utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.util.Utility.sendResponse;

public final class AsyncService implements HttpEntityHandler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    private final DAO dao;
    private final ExecutorService es;

    /**
     * Creates a new {@link AsyncService} with given dao, workers and queueSize.
     * @param dao database controller
     * @param workers count of threads
     * @param queueSize max tasks' count at time
     */
    public AsyncService(@NotNull final DAO dao,
                        final int workers, final int queueSize) {
        this.dao = dao;
        this.es = new ThreadPoolExecutor(
                workers,
                workers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Error in {} when processing request",
                                t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Provide asynchronous handling entity request.
     * @param id key param
     * @param replicas replica configuration
     * @param request processed request
     * @param session current {@link HttpSession} to send response
     * @throws IOException rethrowing from {@link HttpSession#sendResponse}
     */
    public void entity(final String id,
                       final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        final CompletableFuture<Response> op;
        final var timeHeader = Header.getHeader(Utility.TIME_HEADER, request);
        final long time;
        if (timeHeader == null) {
            time = System.currentTimeMillis();
        } else {
            time = Long.parseLong(timeHeader.value);
        }
        final var key = Utility.byteBufferFromString(id);
        try {
            switch (request.getMethod()) {
                case Request.METHOD_DELETE:
                    op = deleteAsync(key, time);
                    break;
                case Request.METHOD_GET:
                    op = getAsync(key);
                    break;
                case Request.METHOD_PUT:
                    op = putAsync(key, ByteBuffer.wrap(request.getBody()), time);
                    break;
                default:
                    logger.error("Unhandled request method");
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
            }
        } catch (RejectedExecutionException e) {
            logger.error("Rejected task", e);
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            return;
        }
        op.whenComplete((r, t) -> {
            if (t == null) {
                sendResponse(session, r);
            } else {
                logger.error("Error in dao operation", t);
                sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        });
    }

    /**
     * Synchronous deleting from dao.
     * @param key key to delete
     * @param time time of deletion
     * @return {@link Response} on deletion
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#removeWithTimeStamp}
     */
    public Response delete(@NotNull final ByteBuffer key,
                           final long time) throws DaoOperationException {
        try {
            dao.removeWithTimeStamp(key, time);
        } catch (IOException e) {
            throw new DaoOperationException("Remove error", e);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    /**
     * Synchronous putting in dao.
     * @param key Record's key
     * @param value Record's value
     * @param time time of putting in dao
     * @return {@link Response} result of putting
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#upsertWithTimeStamp}
     */
    public Response put(@NotNull final ByteBuffer key,
                        @NotNull final ByteBuffer value,
                        final long time) throws DaoOperationException {
        try {
            dao.upsertWithTimeStamp(key, value, time);
        } catch (IOException e) {
            throw new DaoOperationException("Upsert error", e);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * Synchronous get from dao.
     * @param key Record's key
     * @return {@link Response} result of getting
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#getValue}
     */
    public Response get(@NotNull final ByteBuffer key) throws DaoOperationException {
        final Value v;
        try {
            v = dao.getValue(key);
        } catch (IOException e) {
            throw new DaoOperationException("Get error", e);
        }
        if (v.isDead()) {
            final var resp = new Response(Response.NOT_FOUND, Response.EMPTY);
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        } else {
            final var resp = Response.ok(Utility.fromByteBuffer(v.getValue()));
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        }
    }

    /**
     * Asynchronous version of {@link AsyncService#delete}.
     * @param key Record's key
     * @return {@link CompletableFuture<Response>} future result of deleting
     */
    public CompletableFuture<Response> deleteAsync(@NotNull final ByteBuffer key,
                                                   final long time) {
        return CompletableFuture.<Void>supplyAsync(() -> {
            try {
                delete(key, time);
                return null;
            } catch (DaoOperationException e) {
                throw new RuntimeException("IOException in dao.removeWithTimeStamp", e);
            }
        }, es).thenApply(v -> new Response(Response.ACCEPTED, Response.EMPTY));
    }

    /**
     * Asynchronous version of {@link AsyncService#put}.
     * @param key Record's key
     * @param value Record's value
     * @param time time of putting in dao
     * @return {@link CompletableFuture<Response>} future result of putting
     */
    public CompletableFuture<Response> putAsync(@NotNull final ByteBuffer key,
                                                @NotNull final ByteBuffer value,
                                                final long time) {
        return CompletableFuture.<Void>supplyAsync(() -> {
            try {
                put(key, value, time);
                return null;
            } catch (DaoOperationException e) {
                throw new RuntimeException("IOException in dao.upsertWithTimeStamp", e);
            }
        }, es).thenApply(v -> new Response(Response.CREATED, Response.EMPTY));
    }

    /**
     * Asynchronous version of {@link AsyncService#get}.
     * @param key Record's key
     * @return {@link CompletableFuture<Response>} future result of getting
     */
    public CompletableFuture<Response> getAsync(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(key);
            } catch (DaoOperationException e) {
                throw new RuntimeException("IOException in dao.getRAW", e);
            }
        }, es);
    }

    @Override
    public synchronized void close() {
        this.es.shutdown();
    }
}
