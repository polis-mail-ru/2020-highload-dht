package ru.mail.polis.service.manikhin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NettyRequests extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final DAO dao;
    static final String STATUS_PATH = "/v0/status";
    static final String ENTITY_PATH = "/v0/entity";
    static final String ENTITIES_PATH = "/v0/entities";
    private final int clusterSize;

    private static final Logger log = LoggerFactory.getLogger(NettyRequests.class);
    private final Replicas defaultReplica;
    private final Topology nodes;
    private final ReplicasNettyRequests replicaHelper;
    private final Utils utils;

    public NettyRequests(@NotNull final DAO dao, @NotNull final Topology nodes,
                         final int countOfWorkers, final int queueSize, final int timeout) {
        this.dao = dao;
        this.nodes = nodes;
        this.clusterSize = nodes.getNodes().size();
        this.defaultReplica = Replicas.quorum(clusterSize);

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").setUncaughtExceptionHandler((t, e) ->
                        log.error("Error in {} when processing request", t, e)
                ).build(),
                new ThreadPoolExecutor.AbortPolicy());

        Map<String, HttpClient> clusterClients = new HashMap<>();
        for (final String node : nodes.getNodes()) {
            if (!nodes.getId().equals(node) && !clusterClients.containsKey(node)) {
                clusterClients.put(node, HttpClient.newBuilder().executor(executor)
                        .version(HttpClient.Version.HTTP_1_1).build());
            }
        }

        this.replicaHelper = new ReplicasNettyRequests(dao, nodes, clusterClients, executor, timeout);
        this.utils = new Utils(dao, executor, timeout);
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) {
        final String uri = msg.uri();

        if (uri.equals(STATUS_PATH)) {
            Utils.sendResponse(HttpResponseStatus.OK, Utils.EMPTY_BODY, ctx, msg);
            return;
        }

        if (uri.contains(ENTITIES_PATH)) {
            try {
                final QueryStringDecoder decoder = new QueryStringDecoder(uri);
                final List<String> start = decoder.parameters().get("start");
                final List<String> end = decoder.parameters().get("end");

                if (start == null || ((end != null) && end.get(0).isEmpty()) || end == null) {
                    throw new IllegalArgumentException();
                }

                final ByteBuffer from = ByteBuffer.wrap(start.get(0).getBytes(StandardCharsets.UTF_8));
                final ByteBuffer to = ByteBuffer.wrap(end.get(0).getBytes(StandardCharsets.UTF_8));
                final Iterator<Record> iterator = dao.range(from, to);
                StreamNettySession session = new StreamNettySession(iterator, ctx, msg);
                session.startStream();
                return;
            } catch (IllegalArgumentException | IOException error) {
                log.error("IOexception error: ", error);
                Utils.sendResponse(HttpResponseStatus.BAD_REQUEST, Utils.EMPTY_BODY, ctx, msg);
                return;
            }
        }

        if (uri.contains(ENTITY_PATH)) {
            try {
                final QueryStringDecoder decoder = new QueryStringDecoder(uri);
                final List<String> id = decoder.parameters().get("id");

                if (id == null || id.isEmpty() || id.get(0).length() == 0) {
                    Utils.sendResponse(HttpResponseStatus.BAD_REQUEST, Utils.EMPTY_BODY, ctx, msg);
                    return;
                }

                final ByteBuffer key = ByteBuffer.wrap(id.get(0).getBytes(StandardCharsets.UTF_8));
                final boolean isForwardedRequest = msg.headers().contains("X-OK-Proxy");

                if (isForwardedRequest || clusterSize > 1) {
                    final List<String> replicas = decoder.parameters().get("replicas");

                    final Replicas replicaFactor = Replicas.replicaNettyFactor(replicas, ctx, defaultReplica,
                            clusterSize);

                    final Set<String> replicaClusters;

                    if (isForwardedRequest) {
                        replicaClusters = Collections.singleton(nodes.getId());
                    } else {
                        replicaClusters = nodes.getReplicas(key, replicaFactor);
                    }

                    replicaHelper.handleMultiRequest(replicaClusters, msg.retain(), replicaFactor.getAck(), ctx);
                    return;
                } else {
                    final HttpMethod method = msg.method();

                    if (HttpMethod.GET.equals(method)) {
                        utils.localGet(key, ctx, msg);
                        return;
                    } else if (HttpMethod.PUT.equals(method)) {
                        utils.localPut(key, ctx, msg.retain());
                        return;
                    } else if (HttpMethod.DELETE.equals(method)) {
                        utils.localDelete(key, ctx, msg);
                        return;
                    } else {
                        Utils.sendResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, Utils.EMPTY_BODY, ctx, msg);
                        return;
                    }
                }
            } catch (RejectedExecutionException | IOException error) {
                Utils.sendResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, Utils.EMPTY_BODY, ctx, msg);
            }
        } else {
            Utils.sendResponse(HttpResponseStatus.BAD_REQUEST, Utils.EMPTY_BODY, ctx, msg);
            return;
        }
    }
}
