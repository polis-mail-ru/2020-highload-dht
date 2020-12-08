package ru.mail.polis.service.manikhin.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.ByteConvertor;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.manikhin.handlers.DeleteBodyHandler;
import ru.mail.polis.service.manikhin.handlers.GetBodyHandler;
import ru.mail.polis.service.manikhin.handlers.PutBodyHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ServiceUtils {

    private final DAO dao;
    private final ThreadPoolExecutor executor;
    public static final String ENTITY_PATH = "/v0/entity?id=";
    public static final String PROXY_HEADER = "X-OK-Proxy";
    public static final String TIMESTAMP_HEADER = "Timestamp";
    public static final HttpResponseStatus OK = HttpResponseStatus.OK;
    public static final HttpResponseStatus CREATED = HttpResponseStatus.CREATED;
    public static final HttpResponseStatus ACCEPTED = HttpResponseStatus.ACCEPTED;
    public static final HttpResponseStatus GATEWAY_TIMEOUT = HttpResponseStatus.GATEWAY_TIMEOUT;
    public static final HttpResponseStatus INTERNAL_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    public static final HttpResponseStatus NOT_FOUND = HttpResponseStatus.NOT_FOUND;
    public static final byte [] EMPTY_BODY = new byte[0];

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);

    /**
     * Netty service utils.
     *
     * @param dao - storage interface
     * @param executor - thread pool executor for clients
     */
    public ServiceUtils(@NotNull final DAO dao, @NotNull final ThreadPoolExecutor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    /**
     * Response handler for getting record with timestamp from storage by input key.
     *
     * @param key - input record key
     */
    public CompletableFuture<TimestampRecord> getTimestampResponse(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
           try {
               return TimestampRecord.fromBytes(timestampFromByteBuffer(key));
           } catch (NoSuchElementException | IOException error) {
               return TimestampRecord.getEmpty();
           }
        }, executor);
    }

    /**
     * Response handler for getting record from storage by input id.
     *
     * @param id - input id
     * @param ctx - channel handler context
     * @param request - input http request
     */
    public void getResponse(@NotNull final String id, @NotNull final ChannelHandlerContext ctx,
                            @NotNull final FullHttpRequest request) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    final ByteBuffer value = dao.get(key).duplicate();
                    final byte[] valueArray = ByteConvertor.toArray(value);

                    return responseBuilder(OK, valueArray);
                } catch (final IOException error) {
                    log.error("IO get error: ", error);
                    return responseBuilder(INTERNAL_ERROR, EMPTY_BODY);
                } catch (NoSuchElementException error) {
                    log.error("NoSuchElement get error: ", error);
                    return responseBuilder(NOT_FOUND, EMPTY_BODY);
                }
            }, executor)
        );
    }

    /**
     * Handler for getting record iterator by start and end positions.
     *
     * @param start - start position
     * @param end - end position
     */
    public Iterator<Record> getRange(@NotNull ByteBuffer start, ByteBuffer end) throws IOException {
        return dao.range(start, end);
    }

    /**
     * Response handler for insert new record in storage with input id.
     *
     * @param id - input id
     * @param ctx - channel handler context
     * @param request - input http request
     */
    public void putResponse(@NotNull final String id, @NotNull final ChannelHandlerContext ctx,
                            @NotNull final FullHttpRequest request) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    dao.upsert(key, ByteBuffer.wrap(getRequestBody(request.content())));
                    return responseBuilder(CREATED, EMPTY_BODY);
                } catch (IOException | IllegalStateException error) {
                    log.error("IO put error: ", error);
                    return responseBuilder(INTERNAL_ERROR, EMPTY_BODY);
                }
            }, executor)
        );
    }

    /**
     * Response handler for delete record from storage by input key.
     *
     * @param id - input id
     * @param ctx - channel handler context
     * @param request - input http request
     */
    public void deleteResponse(@NotNull final String id, @NotNull final ChannelHandlerContext ctx,
                               @NotNull final FullHttpRequest request) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        respond(ctx, request, CompletableFuture.supplyAsync(() -> {
                try {
                    dao.remove(key);
                    return responseBuilder(ACCEPTED, EMPTY_BODY);
                } catch (IOException | IllegalStateException error) {
                    log.error("IO delete error: ", error);
                    return responseBuilder(INTERNAL_ERROR, EMPTY_BODY);
                }
            }, executor)
        );
    }

    /**
     * Response handler for insert record with timestamp in storage with input key.
     *
     * @param key - input record key
     * @param bytes - input record in the form of bytes
     */
    public CompletableFuture<FullHttpResponse> putTimestampResponse(@NotNull final ByteBuffer key,
                                                                    @NotNull final byte[] bytes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.upsertTimestampRecord(key, ByteBuffer.wrap(bytes));
                return responseBuilder(CREATED, EMPTY_BODY);
            } catch (IOException | IllegalStateException error) {
                return responseBuilder(INTERNAL_ERROR, EMPTY_BODY);
            }
        }, executor);
    }

    /**
     * Response handler for delete record with timestamp from storage by input key.
     *
     * @param key - input record key
     */
    public CompletableFuture<FullHttpResponse> deleteTimestampResponse(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.removeTimestampRecord(key);
                return responseBuilder(ACCEPTED, EMPTY_BODY);
            } catch (IOException error) {
                throw new RuntimeException("Get future error", error);
            }
        }, executor);
    }

    /**
     * Proxy Response handler for getting record with timestamp from storage.
     *
     * @param client - input HTTP-client
     * @param node - input node
     * @param id - input record id
     */
    public CompletableFuture<TimestampRecord> getProxyResponse(@NotNull final HttpClient client,
                                                               @NotNull final String node,
                                                               @NotNull final String id) {

        final HttpRequest request = requestBuilder(node, id).GET().build();

        return client.sendAsync(request, GetBodyHandler.INSTANCE).thenApplyAsync(HttpResponse::body, executor);
    }

    /**
     * Proxy Response handler for delete record with timestamp from storage.
     *
     * @param client - input HTTP-client
     * @param node - input node
     * @param id - input record id
     */
    public CompletableFuture<FullHttpResponse> deleteProxyResponse(@NotNull final HttpClient client,
                                                                   @NotNull final String node,
                                                                   @NotNull final String id) {

        final HttpRequest request = requestBuilder(node, id).DELETE().build();

        return client.sendAsync(request, DeleteBodyHandler.INSTANCE).thenApplyAsync(r -> responseBuilder(
                ACCEPTED, EMPTY_BODY), executor);
    }

    /**
     * Proxy Response handler for insert record with timestamp in storage.
     *
     * @param client - input HTTP-client
     * @param node - input node
     * @param id - input record id
     */
    public CompletableFuture<FullHttpResponse> putProxyResponse(@NotNull final HttpClient client,
                                                                @NotNull final String node,
                                                                @NotNull final String id,
                                                                @NotNull final byte[] value) {

        final HttpRequest request = requestBuilder(node, id).PUT(
                HttpRequest.BodyPublishers.ofByteArray(value)
        ).build();

        return client.sendAsync(request, PutBodyHandler.INSTANCE).thenApplyAsync(r -> responseBuilder(
                CREATED, EMPTY_BODY), executor);
    }

    /**
     * Timestamp records convertor to bytes.
     *
     * @param key - input ByteBuffer key
     */
    public byte[] timestampFromByteBuffer(@NotNull final ByteBuffer key) throws IOException {
        final TimestampRecord res = dao.getTimestampRecord(key);

        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }

        return res.toBytes();
    }

    /**
     * Request builder.
     *
     * @param node - input node
     * @param id - input record id
     */
    public HttpRequest.Builder requestBuilder(@NotNull final String node, @NotNull final String id) {

        return HttpRequest.newBuilder().uri(URI.create(node + ENTITY_PATH + id)).header(PROXY_HEADER, "True");
    }

    /**
     * ByteBuf body convertor to bytes.
     *
     * @param buffer - input ByteBuf request body
     */
    public static byte[] getRequestBody(final ByteBuf buffer) {

        final ByteBuf bufferCopy = buffer.retainedDuplicate();
        final byte[] array = new byte[bufferCopy.readableBytes()];
        bufferCopy.readBytes(array);

        return array;
    }

    /**
     * Response builder.
     *
     * @param status - response status
     * @param bytes - input message in the form of bytes
     */
    public static FullHttpResponse responseBuilder(@NotNull final HttpResponseStatus status,
                                                   @NotNull final byte[] bytes) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return response;
    }

    /** Create future of input requests results.
     *
     * @param futures - list of futures with requests results.
     * @param successes - necessary number of successful requests
     * @param isForwarded - check processed request on forwarding
     * */
    public <T> CompletableFuture<Collection<T>> atLeastAsync(@NotNull final Collection<CompletableFuture<T>> futures,
                                                             final int successes, final boolean isForwarded) {

        final AtomicInteger successLeft = new AtomicInteger(successes);
        final AtomicInteger errorsLeft = new AtomicInteger(futures.size() - successes + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();

        futures.forEach(f -> f.whenCompleteAsync((v, t) -> {
            if (t == null) {
                results.add(v);

                if (successLeft.decrementAndGet() == 0 || isForwarded) {
                    future.complete(results);
                }
            } else {
                if (errorsLeft.decrementAndGet() == 0) {
                    future.completeExceptionally(new IllegalStateException("Can't get " + successes + " values"));
                }
            }
        }, executor).isCancelled());

        return future;
    }

    /** Handler to processing final future response.
     *
     * @param ctx - channel handler context.
     * @param request - processed request
     * @param response - input future response
     * */
    public void respond(@NotNull final ChannelHandlerContext ctx, @NotNull final FullHttpRequest request,
                        @NotNull final CompletableFuture<FullHttpResponse> response) {
        response.whenComplete((r, t) -> {
            if (t == null) {
                if (HttpUtil.isKeepAlive(request)) {
                    r.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                } else {
                    r.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                }

                ctx.writeAndFlush(r).isCancelled();
            } else {
                final HttpResponseStatus code;

                if (t instanceof CompletionException) {
                    t = t.getCause();
                }

                if (t instanceof IllegalStateException) {
                    code = GATEWAY_TIMEOUT;
                } else {
                    code = INTERNAL_ERROR;
                }

                sendResponse(code, t.getMessage().getBytes(StandardCharsets.UTF_8), ctx, request);
            }
        }).isCancelled();
    }

    /** Handler to sending http responses.
     *
     * @param status - http status code sending response
     * @param bytes - input message in the form of bytes
     * @param ctx - channel handler context
     * @param request - processed request
     * */
    public static void sendResponse(@NotNull final HttpResponseStatus status, @NotNull final byte[] bytes,
                                    @NotNull final ChannelHandlerContext ctx, @NotNull final FullHttpRequest request) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ctx.writeAndFlush(response).isCancelled();
    }

    /** Forward checker.
     *
     * @param request - input HTTP request
     * */
    public boolean isForwarded(@NotNull final FullHttpRequest request) {
        return request.headers().contains(ServiceUtils.PROXY_HEADER);
    }
}
