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

    public void entity(final String id,
                       final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        final CompletableFuture<Response> op;
        final var timeHeader = Header.getHeader(Utility.TIME_HEADER, request);
        final long time;
        if (timeHeader != null) {
            time = Long.parseLong(timeHeader.value);
        } else {
            time = System.currentTimeMillis();
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

    public Response delete(@NotNull final ByteBuffer key,
                           final long time) throws DaoOperationException {
        try {
            dao.removeWithTimeStamp(key, time);
        } catch (IOException e) {
            throw new DaoOperationException("Remove error");
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    public Response put(@NotNull final ByteBuffer key,
                        @NotNull final ByteBuffer value,
                        final long time) throws DaoOperationException {
        try {
            dao.upsertWithTimeStamp(key, value, time);
        } catch (IOException e) {
            throw new DaoOperationException("Upsert error");
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    public Response get(@NotNull final ByteBuffer key) throws DaoOperationException {
        final Value v;
        try {
            v = dao.getValue(key);
        } catch (IOException e) {
            throw new DaoOperationException("Get error");
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
