package ru.mail.polis.service.manikhin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.ByteConvertor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyRequests extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final DAO dao;
    static final String STATUS_PATH = "/v0/status";
    static final String ENTITY_PATH = "/v0/entity";
    private final int clusterSize;

    private final Logger log = LoggerFactory.getLogger(NettyRequests.class);
    private static final String SUCCESS_MESSAGE = "200";
    private final Replicas defaultReplica;
    private final Topology nodes;
    private final ReplicasNettyRequests replHelper;

    public NettyRequests(@NotNull final DAO dao, @NotNull final Topology nodes, final int timeout) {
        this.dao = dao;
        this.nodes = nodes;

        this.clusterSize = nodes.getNodes().size();
        this.defaultReplica = Replicas.quorum(clusterSize);
        this.replHelper = new ReplicasNettyRequests(dao, nodes, timeout);

    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) {
        final String uri = msg.uri();

        if (uri.equals(STATUS_PATH)) {
            sendResponse(HttpResponseStatus.OK,
                    SUCCESS_MESSAGE.getBytes(StandardCharsets.UTF_8), ctx);
            return;
        }

        if (uri.contains(ENTITY_PATH)) {
            try {
                final QueryStringDecoder decoder = new QueryStringDecoder(uri);
                final List<String> id = decoder.parameters().get("id");

                if (id == null || id.isEmpty() || id.get(0).length() == 0) {
                    sendResponse(HttpResponseStatus.BAD_REQUEST, new byte[0], ctx);
                    return;
                }

                final ByteBuffer key = ByteBuffer.wrap(id.get(0).getBytes(StandardCharsets.UTF_8));
                final boolean isForwardedRequest = msg.headers().contains("X-OK-Proxy");

                if (isForwardedRequest || clusterSize > 1) {
                    final List<String> replicas = decoder.parameters().get("replicas");

                    final Replicas replicaFactor = Replicas.replicaNettyFactor(replicas, ctx, defaultReplica,
                            clusterSize);

                    final Set<String> replicaClusters = isForwardedRequest ? Collections.singleton(nodes.getId())
                            : nodes.getReplicas(key, replicaFactor);

                    replHelper.handleMultiRequest(replicaClusters, msg, replicaFactor.getAck(), ctx);
                    return;
                } else {
                    final HttpMethod method = msg.method();

                    if (HttpMethod.GET.equals(method)) {
                        get(key, ctx);
                        return;
                    } else if (HttpMethod.PUT.equals(method)) {
                        put(key, msg, ctx);
                        return;
                    } else if (HttpMethod.DELETE.equals(method)) {
                        delete(key, ctx);
                        return;
                    } else {
                        sendResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, new byte[0], ctx);
                        return;
                    }
                }
            } catch (RejectedExecutionException | IOException error) {
                sendResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, new byte[0], ctx);
            }
        } else {
            sendResponse(HttpResponseStatus.BAD_REQUEST, new byte[0], ctx);
            return;
        }
    }

    private void get(@NotNull final ByteBuffer key, @NotNull final ChannelHandlerContext ctx) {
        try {
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] valueArray = ByteConvertor.toArray(value);

            sendResponse(HttpResponseStatus.OK, valueArray, ctx);
        } catch (final IOException error) {
            log.error("IO get error: ", error);
            sendResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, new byte[0], ctx);
        } catch (NoSuchElementException error) {
            log.error("NoSuchElement get error: ", error);
            sendResponse(HttpResponseStatus.NOT_FOUND, new byte[0], ctx);
        }
    }

    private void put(final ByteBuffer key, @NotNull final FullHttpRequest request,
                     @NotNull final ChannelHandlerContext ctx) {
        try {
            dao.upsert(key, ByteBuffer.wrap(getRequestBody(request.content().retain())));
            sendResponse(HttpResponseStatus.CREATED, new byte[0], ctx);
        } catch (IOException error) {
            log.error("IO put error: ", error);
            sendResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, new byte[0], ctx);
        }
    }

    private void delete(@NotNull final ByteBuffer key,
                        @NotNull final ChannelHandlerContext ctx) {
        try {
            dao.remove(key);
            sendResponse(HttpResponseStatus.ACCEPTED, new byte[0], ctx);
        } catch (IOException error) {
            log.error("IO delete error: ", error);
            sendResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, new byte[0], ctx);
        }
    }

    public static byte[] getRequestBody(final ByteBuf buffer) {
        final ByteBuf bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.readableBytes()];

        bufferCopy.getBytes(0, array).clear();
        return array;
    }

    private void sendResponse(final @NotNull HttpResponseStatus status,
                              final @NotNull byte[] bytes,
                              final @NotNull ChannelHandlerContext ctx) {

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Something wrong with written some data.");
            }
        });
    }
}
