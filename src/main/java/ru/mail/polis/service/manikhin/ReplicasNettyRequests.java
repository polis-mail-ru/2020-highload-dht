package ru.mail.polis.service.manikhin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.TimestampRecord;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ReplicasNettyRequests {
    private final DAO dao;
    private final Topology nodes;
    private final int timeout;
    private final Logger log = LoggerFactory.getLogger(ReplicasNettyRequests.class);
    private static final String PROXY_HEADER = "X-OK-Proxy";
    private static final String ENTITY_PATH = "/v0/entity?id=";

    ReplicasNettyRequests(final DAO dao, final Topology nodes, final int timeout) {
        this.dao = dao;
        this.nodes = nodes;
        this.timeout = timeout;
    }

    /**
     * get record with timestamp by input key.
     *
     * @param key - input ByteBuffer key
     * @return Response
     */
    public FullHttpResponse getTimestamp(@NotNull final ByteBuffer key) {
        try {
            final byte[] res = timestampFromByteBuffer(key);
            return responseBuilder(HttpResponseStatus.OK, res);
        } catch (NoSuchElementException | IOException exp) {
            return responseBuilder(HttpResponseStatus.NOT_FOUND, new byte[0]);
        }
    }

    /**
     * put record with timestamp with input key.
     *
     * @param key - input ByteBuffer key
     */
    public void putTimestamp(@NotNull final ByteBuffer key,
                             @NotNull final FullHttpRequest request) throws IOException {
        dao.upsertTimestampRecord(key, ByteBuffer.wrap(getRequestBody(request.content())));
    }

    public void deleteTimestamp(@NotNull final ByteBuffer key) throws IOException {
        dao.removeTimestampRecord(key);
    }

    /**
     * Timestamp records convertor to bytes.
     *
     * @param key - input ByteBuffer key
     */
    public byte[] timestampFromByteBuffer(@NotNull final ByteBuffer key)
            throws IOException {
        final TimestampRecord res = dao.getTimestampRecord(key);

        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }
        return res.toBytes();
    }

    /**
     * Request handler for input requests with many replicas.
     *
     * @param replicaClusters - replica clusters
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     * @param ctx - http-session
     */
    public void handleMultiRequest(@NotNull final Set<String> replicaClusters,
                                   @NotNull final FullHttpRequest request,
                                   final int replicateAcks,
                                   @NotNull final ChannelHandlerContext ctx) {
        try {
            final HttpMethod method = request.method();

            if (HttpMethod.GET.equals(method)) {
                multiGet(ctx, replicaClusters, request, replicateAcks);
                return;
            } else if (HttpMethod.PUT.equals(method)) {
                multiPut(ctx, replicaClusters, request, replicateAcks);
                return;
            } else if (HttpMethod.DELETE.equals(method)) {
                multiDelete(ctx, replicaClusters, request, replicateAcks);
                return;
            } else {
                sendNettyResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, new byte[0], ctx);
                return;
            }
        } catch (IOException error) {
            log.error("handleMultiRequest error: ", error);
            sendNettyResponse(HttpResponseStatus.GATEWAY_TIMEOUT, new byte[0], ctx);
            return;
        }
    }

    /**
     * Request handler for input GET-request with many replicas.
     *
     * @param ctx - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiGet(@NotNull final ChannelHandlerContext ctx,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final FullHttpRequest request,
                         final int replicateAcks) throws IOException {
        final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        final String id = decoder.parameters().get("id").get(0);
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;
        final List<TimestampRecord> responses = new ArrayList<>();

        for (final String node : replicaNodes) {
            try {
                FullHttpResponse respGet;

                if (node.equals(nodes.getId())) {
                    respGet = getTimestamp(key);
                } else {
                    final URI requestUri = new URI(node + ENTITY_PATH + id);
                    final HttpResponse response = Request.Get(requestUri).addHeader(PROXY_HEADER, "True")
                            .socketTimeout(timeout).connectTimeout(timeout).execute().returnResponse();
                    final HttpResponseStatus code;

                    if (response.getStatusLine().getStatusCode() == 200) {
                        code = HttpResponseStatus.OK;
                    } else {
                        code = HttpResponseStatus.NOT_FOUND;
                    }

                    respGet = responseBuilder(code, EntityUtils.toByteArray(response.getEntity()));
                }

                responses.add(TimestampRecord.fromBytes(getRequestBody(respGet.content())));

                asks++;
            } catch (URISyntaxException | HttpResponseException | HttpHostConnectException error) {
                log.error("multiGet error", error);
            }
        }

        if (asks >= replicateAcks || isForwardedRequest) {
            processResponses(ctx, replicaNodes, responses, isForwardedRequest);
        } else {
            sendNettyResponse(HttpResponseStatus.GATEWAY_TIMEOUT, new byte[0], ctx);
        }
    }

    /**
     * Request handler for input PUT-request with many replicas.
     *
     * @param ctx - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiPut(@NotNull final ChannelHandlerContext ctx,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final FullHttpRequest request,
                         final int replicateAcks) throws IOException {
        final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        final String id = decoder.parameters().get("id").get(0);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    putTimestamp(key, request);
                    asks++;
                } else {
                    final URI requestUri = new URI(node + ENTITY_PATH + id);
                    final Response response = Request.Put(requestUri).addHeader(PROXY_HEADER, "True")
                            .socketTimeout(timeout).connectTimeout(timeout)
                            .bodyByteArray(getRequestBody(request.content())).execute();

                    HttpResponse r = response.returnResponse();
                    asks += r.getStatusLine().getStatusCode() == 201 ? 1 : 0;
                }
            } catch (URISyntaxException | HttpResponseException | HttpHostConnectException error) {
                log.error("multiPut error", error);
            }
        }

        if (asks >= replicateAcks || isForwardedRequest) {
            sendNettyResponse(HttpResponseStatus.CREATED, new byte[0], ctx);
        } else {
            sendNettyResponse(HttpResponseStatus.GATEWAY_TIMEOUT, new byte[0], ctx);
        }
    }

    /**
     * Request handler for input DELETE-request with many replicas.
     *
     * @param ctx - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiDelete(@NotNull final ChannelHandlerContext ctx,
                            @NotNull final Set<String> replicaNodes,
                            @NotNull final FullHttpRequest request,
                            final int replicateAcks) throws IOException {
        final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        final String id = decoder.parameters().get("id").get(0);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    deleteTimestamp(key);
                    asks++;
                } else {
                    final URI requestUri = new URI(node + ENTITY_PATH + id);

                    final Response response = Request.Delete(requestUri).addHeader(PROXY_HEADER, "True")
                            .socketTimeout(timeout).connectTimeout(timeout).execute();

                    HttpResponse r = response.returnResponse();
                    asks += r.getStatusLine().getStatusCode() == 202 ? 1 : 0;
                }
            } catch (URISyntaxException | HttpHostConnectException | HttpResponseException error) {
                log.error("multiDelete error: ", error);
            }
        }

        log.debug("Res after deleting: " + asks + "/" + replicateAcks);
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);

        if (asks >= replicateAcks || isForwardedRequest) {
            sendNettyResponse(HttpResponseStatus.ACCEPTED, new byte[0], ctx);
        } else {
            sendNettyResponse(HttpResponseStatus.GATEWAY_TIMEOUT, new byte[0], ctx);
        }
    }

    /**
     * Joiner input set responses for one.
     *
     * @param ctx - http-session
     * @param replicaNodes - replica nodes
     * @param responses - input set with responses
     * @param isForwardedRequest - check result request on forwarding
     */
    public void processResponses(@NotNull final ChannelHandlerContext ctx,
                                 @NotNull final Set<String> replicaNodes,
                                 @NotNull final List<TimestampRecord> responses,
                                 final boolean isForwardedRequest) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);

        if (mergedResp.isValue()) {
            if (isForwardedRequest && replicaNodes.size() == 1) {
                sendNettyResponse(HttpResponseStatus.OK, mergedResp.toBytes(), ctx);
            } else if (!isForwardedRequest && replicaNodes.size() == 1) {
                sendNettyResponse(HttpResponseStatus.OK, mergedResp.getValueAsBytes(), ctx);
            } else {
                sendNettyResponse(HttpResponseStatus.OK, mergedResp.getValueAsBytes(), ctx);
            }
        } else if (mergedResp.isDeleted()) {
            sendNettyResponse(HttpResponseStatus.NOT_FOUND, mergedResp.toBytes(), ctx);
        } else {
            sendNettyResponse(HttpResponseStatus.NOT_FOUND, mergedResp.toBytes(), ctx);
        }
    }

    private void sendNettyResponse(final @NotNull HttpResponseStatus status,
                                   final @NotNull byte[] bytes,
                                   final @NotNull ChannelHandlerContext ctx) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    private FullHttpResponse responseBuilder(final @NotNull HttpResponseStatus status,
                                             final @NotNull byte[] bytes) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        return response;
    }

    public static byte[] getRequestBody(final ByteBuf buffer) {
        final ByteBuf bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.readableBytes()];

        bufferCopy.readBytes(array);
        return array;
    }
}
