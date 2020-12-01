package ru.mail.polis.service.manikhin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.ByteConvertor;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.manikhin.handlers.GetBodyHandler;
import ru.mail.polis.service.manikhin.handlers.deleteBodyHandler;
import ru.mail.polis.service.manikhin.handlers.putBodyHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Utils {

    private final DAO dao;
    private final ThreadPoolExecutor executor;
    private static final String ENTITY_PATH = "/v0/entity?id=";
    private static final String PROXY_HEADER = "X-OK-Proxy";
    public static final byte [] EMPTY_BODY = new byte[0];

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private final int timeout;

    public Utils (@NotNull final DAO dao, @NotNull final ThreadPoolExecutor executor, final int timeout) {
        this.dao = dao;
        this.executor = executor;
        this.timeout = timeout;
    }

    public CompletableFuture<TimestampRecord> getResponse(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
           try {
               return TimestampRecord.fromBytes(timestampFromByteBuffer(key));
           } catch (NoSuchElementException | IOException error) {
               return TimestampRecord.getEmpty();
           }
        }, executor);
    }

    public void localGet(@NotNull final ByteBuffer key, @NotNull ChannelHandlerContext ctx,
                         @NotNull FullHttpRequest request) {
        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    final ByteBuffer value = dao.get(key).duplicate();
                    final byte[] valueArray = ByteConvertor.toArray(value);

                    return responseBuilder(HttpResponseStatus.OK, valueArray);
                } catch (final IOException error) {
                    log.error("IO get error: ", error);
                    return responseBuilder(HttpResponseStatus.INTERNAL_SERVER_ERROR, EMPTY_BODY);
                } catch (NoSuchElementException error) {
                    log.error("NoSuchElement get error: ", error);
                    return responseBuilder(HttpResponseStatus.NOT_FOUND, EMPTY_BODY);
                }
            }, executor)
        );

        return;
    }

    public void localPut(@NotNull final ByteBuffer key, @NotNull ChannelHandlerContext ctx,
                         @NotNull final FullHttpRequest request) {
        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    dao.upsert(key, ByteBuffer.wrap(Utils.getRequestBody(request.content())));
                    return responseBuilder(HttpResponseStatus.CREATED, EMPTY_BODY);
                } catch (IOException | IllegalStateException error) {
                    log.error("IO put error: ", error);
                    return responseBuilder(HttpResponseStatus.INTERNAL_SERVER_ERROR, EMPTY_BODY);
                }
            }, executor)
        );

        return;
    }

    public void localDelete(@NotNull final ByteBuffer key, ChannelHandlerContext ctx,
                            @NotNull final FullHttpRequest request) {
        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    dao.remove(key);
                    return responseBuilder(HttpResponseStatus.ACCEPTED, EMPTY_BODY);
                } catch (IOException error) {
                    log.error("IO delete error: ", error);
                    return responseBuilder(HttpResponseStatus.INTERNAL_SERVER_ERROR, EMPTY_BODY);
                }
            }, executor)
        );

        return;
    }

    public CompletableFuture<FullHttpResponse> putResponse(@NotNull final ByteBuffer key,
                                                           @NotNull final byte[] bytes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.upsertTimestampRecord(key, ByteBuffer.wrap(bytes));
                return responseBuilder(HttpResponseStatus.CREATED, EMPTY_BODY);
            } catch (IOException error) {
                return responseBuilder(HttpResponseStatus.INTERNAL_SERVER_ERROR, EMPTY_BODY);
            }
        }, executor);
    }

    public CompletableFuture<FullHttpResponse> deleteResponse(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.removeTimestampRecord(key);
                return responseBuilder(HttpResponseStatus.ACCEPTED, EMPTY_BODY);
            } catch (IOException error) {
                throw new RuntimeException("Get future error", error);
            }
        }, executor);
    }

    public CompletableFuture<TimestampRecord> getProxyResponse(final Map<String, HttpClient> clusterClients,
                                                               @NotNull final String node,
                                                               @NotNull final String id) {

        final HttpRequest request = requestBuilder(node, id).GET().build();

        return clusterClients.get(node).sendAsync(request, GetBodyHandler.INSTANCE)
                .thenApplyAsync(HttpResponse::body, executor);
    }

    public CompletableFuture<FullHttpResponse> deleteProxyResponse(final Map<String, HttpClient> clusterClients,
                                                                   @NotNull final String node,
                                                                   @NotNull final String id) {

        final HttpRequest request = requestBuilder(node, id).DELETE().build();

        return clusterClients.get(node).sendAsync(request, deleteBodyHandler.INSTANCE)
                .thenApplyAsync(r -> responseBuilder(HttpResponseStatus.ACCEPTED, EMPTY_BODY), executor);
    }

    public CompletableFuture<FullHttpResponse> putProxyResponse(final Map<String, HttpClient> clusterClients,
                                                                @NotNull final String node,
                                                                @NotNull final String id,
                                                                @NotNull final byte[] value) {

        final HttpRequest request = requestBuilder(node, id).PUT(
                HttpRequest.BodyPublishers.ofByteArray(value)
        ).build();

        return clusterClients.get(node).sendAsync(request, putBodyHandler.INSTANCE)
                .thenApplyAsync(r -> responseBuilder(HttpResponseStatus.CREATED, EMPTY_BODY), executor);
    }

    public byte[] timestampFromByteBuffer(@NotNull final ByteBuffer key) throws IOException {
        final TimestampRecord res = dao.getTimestampRecord(key);

        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }

        return res.toBytes();
    }

    public HttpRequest.Builder requestBuilder(@NotNull final String node, @NotNull final String id) {

        final String uri = node + ENTITY_PATH + id;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 

        return HttpRequest.newBuilder().uri(URI.create(uri)).header(PROXY_HEADER, "True")
                .timeout(Duration.ofMillis(timeout));
    }

    public static byte[] getRequestBody(final ByteBuf buffer) {

        final ByteBuf bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.readableBytes()];
        bufferCopy.readBytes(array);

        return array;
    }

    static FullHttpResponse responseBuilder(@NotNull HttpResponseStatus status, @NotNull final byte[] bytes) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return response;
    }

    public <T> CompletableFuture<Collection<T>> atLeastAsync(@NotNull final Collection<CompletableFuture<T>> futures,
                                                             final int successes, final boolean isForwarded) {

        final AtomicInteger errorsLeft = new AtomicInteger(0);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();

        futures.forEach(f -> f.whenCompleteAsync((v, t) -> {
            if (t == null) {
                results.add(v);

                if (results.size() >= successes || isForwarded) {
                    future.complete(results);
                }
            } else {
                if (errorsLeft.incrementAndGet() >= (futures.size() - successes + 1)) {
                    future.completeExceptionally(new IllegalStateException("Can't get " + successes + " values"));
                    return;
                }
            }
        }, executor).isCancelled());

        return future;
    }

    public void respond(@NotNull final ChannelHandlerContext ctx, @NotNull FullHttpRequest request,
                        @NotNull final CompletableFuture<FullHttpResponse> response) {
        response.whenComplete((r, t) -> {
            if (t == null) {
                ctx.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE);
                return;
            } else {
                final HttpResponseStatus code;

                if (t instanceof CompletionException) {
                    t = t.getCause();
                }

                if (t instanceof IllegalStateException) {
                    code = HttpResponseStatus.GATEWAY_TIMEOUT;
                } else {
                    code = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                }

                sendResponse(code, t.getMessage().getBytes(StandardCharsets.UTF_8), ctx, request);
            }
        }).isCancelled();
    }

    public static void sendResponse(final @NotNull HttpResponseStatus status, final @NotNull byte[] bytes,
                                   final @NotNull ChannelHandlerContext ctx, FullHttpRequest request) {
        final boolean isKeepAlive = HttpUtil.isKeepAlive(request);

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        if (isKeepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
