package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

public class NettyRequests extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final HttpResponseStatus BAD_REQUEST = HttpResponseStatus.BAD_REQUEST;
    private static final HttpResponseStatus METHOD_NOT_ALLOWED = HttpResponseStatus.METHOD_NOT_ALLOWED;
    private static final HttpResponseStatus SERVICE_UNAVAILABLE = HttpResponseStatus.SERVICE_UNAVAILABLE;
    private static final String STATUS_PATH = "/v0/status";
    private static final String ENTITIES_PATH = "/v0/entities";
    private static final String ENTITY_PATH = "/v0/entity";
    private final int clusterSize;

    private static final Logger log = LoggerFactory.getLogger(NettyRequests.class);
    private final ServiceUtils serviceUtils;
    private final ReplicasNettyRequests replicaHelper;

    /**
     * Request handlers for netty async service implementation.
     *
     */
    public NettyRequests(@NotNull final ReplicasNettyRequests replicaHelper, @NotNull ServiceUtils utils,
                         final int clusterSize) {
        this.clusterSize = clusterSize;
        this.serviceUtils = utils;
        this.replicaHelper = replicaHelper;
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) {
        final String uri = msg.uri();

        if (uri.equals(STATUS_PATH)) {
            ServiceUtils.sendResponse(HttpResponseStatus.OK, ServiceUtils.EMPTY_BODY, ctx, msg);
        } else if (uri.startsWith(ENTITIES_PATH)) {
            entitiesHandler(ctx, msg, uri);
        } else if (uri.startsWith(ENTITY_PATH)) {
            entityHandler(ctx, msg, uri);
        } else {
            serviceUtils.respond(ctx, msg, CompletableFuture.supplyAsync(() -> ServiceUtils.responseBuilder(
                    BAD_REQUEST, ServiceUtils.EMPTY_BODY)));
        }
    }

    private void entitiesHandler(@NotNull final ChannelHandlerContext ctx, @NotNull final FullHttpRequest request,
                                 @NotNull final String uri) {
        try {
            final QueryStringDecoder decoder = new QueryStringDecoder(uri);
            final List<String> start = decoder.parameters().get("start");
            final List<String> end = decoder.parameters().get("end");

            if (start == null || ((end != null) && end.get(0).isEmpty())) {
                throw new IllegalArgumentException("start or end null!");
            }

            final ByteBuffer from = ByteBuffer.wrap(start.get(0).getBytes(StandardCharsets.UTF_8));
            final ByteBuffer to = (end == null) ? null : ByteBuffer.wrap(end.get(0).getBytes(StandardCharsets.UTF_8));

            final Iterator<Record> iterator = serviceUtils.getRange(from, to);
            final StreamNettySession session = new StreamNettySession(iterator);
            session.startStream(ctx);
        } catch (IllegalArgumentException | IOException error) {
            log.error("IO exception error: ", error);
            ServiceUtils.sendResponse(BAD_REQUEST, ServiceUtils.EMPTY_BODY, ctx, request);
        }
    }

    private void entityHandler(@NotNull final ChannelHandlerContext ctx, @NotNull final FullHttpRequest request,
                               @NotNull final String uri) {
        try {
            final QueryStringDecoder decoder = new QueryStringDecoder(uri);
            final List<String> id = decoder.parameters().get("id");

            if (id == null || id.isEmpty() || id.get(0).length() == 0) {
                serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() -> ServiceUtils.responseBuilder(
                        BAD_REQUEST, ServiceUtils.EMPTY_BODY)));
                return;
            }

            final List<String> replicas = decoder.parameters().get("replicas");
            final Replicas replicaFactor = Replicas.replicaNettyFactor(replicas, clusterSize);
            final String idValue = id.get(0);

            switch (request.method().toString()) {
                case "GET":
                    getRequest(ctx, request, replicaFactor, idValue);
                    break;
                case "PUT":
                    putRequest(ctx, request, replicaFactor, idValue);
                    break;
                case "DELETE":
                    deleteRequest(ctx, request, replicaFactor, idValue);
                    break;
                default:
                    serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() ->
                            ServiceUtils.responseBuilder(METHOD_NOT_ALLOWED, ServiceUtils.EMPTY_BODY)
                    ));
                    break;
            }
        } catch (IllegalArgumentException error) {
            serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() -> ServiceUtils.responseBuilder(
                    BAD_REQUEST, ServiceUtils.EMPTY_BODY)));
        } catch (RejectedExecutionException error) {
            serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() -> ServiceUtils.responseBuilder(
                    SERVICE_UNAVAILABLE, ServiceUtils.EMPTY_BODY)));
        }
    }

    private void getRequest(@NotNull final ChannelHandlerContext context, @NotNull final FullHttpRequest request,
                            @NotNull final Replicas replicaFactor, @NotNull final String id) {
        if (clusterSize > 1) {
            replicaHelper.multiGet(context, replicaFactor, request, id);
        } else {
            serviceUtils.getResponse(id, context, request);
        }
    }

    private void putRequest(@NotNull final ChannelHandlerContext context, @NotNull final FullHttpRequest request,
                            @NotNull final Replicas replicaFactor, @NotNull final String id) {
        if (clusterSize > 1) {
            replicaHelper.multiPut(context, replicaFactor, request, id);
        } else {
            serviceUtils.putResponse(id, context, request.retain());
        }
    }

    private void deleteRequest(@NotNull final ChannelHandlerContext context, @NotNull final FullHttpRequest request,
                               @NotNull final Replicas replicaFactor, @NotNull final String id) {
        if (clusterSize > 1) {
            replicaHelper.multiDelete(context, replicaFactor, request, id);
        } else {
            serviceUtils.deleteResponse(id, context, request);
        }
    }
}
