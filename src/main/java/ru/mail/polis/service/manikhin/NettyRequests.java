package ru.mail.polis.service.manikhin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NettyRequests extends SimpleChannelInboundHandler<FullHttpRequest> {
    static final String STATUS_PATH = "/v0/status";
    static final String ENTITY_PATH = "/v0/entity";
    private final int clusterSize;

    private static final Logger log = LoggerFactory.getLogger(NettyRequests.class);
    private final Replicas defaultReplica;
    private final Topology nodes;
    private final ReplicasNettyRequests replicaHelper;
    private final ServiceUtils serviceUtils;

    /**
     * Request handlers for netty async service implementation.
     *
     * @param dao - storage interface
     * @param nodes - nodes list
     * @param countOfWorkers - count of workers
     * @param queueSize - queue size
     * @param timeout - init timeout for http clients
     */
    public NettyRequests(@NotNull final DAO dao, @NotNull final Topology nodes, final int countOfWorkers,
                         final int queueSize, final int timeout) {
        this.nodes = nodes;
        this.clusterSize = nodes.getNodes().size();
        this.defaultReplica = Replicas.quorum(clusterSize);

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").setUncaughtExceptionHandler((t, e) ->
                        log.error("Error in {} when processing request", t, e)
                ).build(),
                new ThreadPoolExecutor.AbortPolicy());

        final Map<String, HttpClient> clusterClients = new HashMap<>();

        for (final String node : nodes.getNodes()) {
            if (!nodes.getId().equals(node) && !clusterClients.containsKey(node)) {
                clusterClients.put(node, HttpClient.newBuilder().executor(executor)
                        .version(HttpClient.Version.HTTP_1_1).build());
            }
        }

        this.replicaHelper = new ReplicasNettyRequests(dao, nodes, clusterClients, executor, timeout);
        this.serviceUtils = new ServiceUtils(dao, executor, timeout);
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
        } else if (uri.contains(ENTITY_PATH)) {
            entityHandler(ctx, msg.retain(), uri);
        } else {
            serviceUtils.respond(ctx, msg, CompletableFuture.supplyAsync(() ->
                    ServiceUtils.responseBuilder(HttpResponseStatus.BAD_REQUEST, ServiceUtils.EMPTY_BODY)));
        }
    }

    private void entityHandler(@NotNull final ChannelHandlerContext ctx,
                               @NotNull final FullHttpRequest request, @NotNull final String uri) {
        try {
            final QueryStringDecoder decoder = new QueryStringDecoder(uri);
            final List<String> id = decoder.parameters().get("id");

            if (id == null || id.isEmpty() || id.get(0).length() == 0) {
                serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() ->
                        ServiceUtils.responseBuilder(HttpResponseStatus.BAD_REQUEST, ServiceUtils.EMPTY_BODY)));
                return;
            }

            final ByteBuffer key = ByteBuffer.wrap(id.get(0).getBytes(StandardCharsets.UTF_8));
            final boolean isForwardedRequest = request.headers().contains("X-OK-Proxy");

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

                replicaHelper.handleMultiRequest(replicaClusters, request.retain(), replicaFactor.getAck(), ctx);
                return;
            }

            switch (request.method().toString()) {
                case "GET":
                    serviceUtils.getResponse(key, ctx, request);
                    break;
                case "PUT":
                    serviceUtils.putResponse(key, ctx, request);
                    break;
                case "DELETE":
                    serviceUtils.deleteResponse(key, ctx, request);
                    break;
                default:
                    serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() ->
                            ServiceUtils.responseBuilder(HttpResponseStatus.METHOD_NOT_ALLOWED,
                                    ServiceUtils.EMPTY_BODY)));
                    break;
            }
        } catch (RejectedExecutionException error) {
            serviceUtils.respond(ctx, request, CompletableFuture.supplyAsync(() ->
                    ServiceUtils.responseBuilder(HttpResponseStatus.SERVICE_UNAVAILABLE, ServiceUtils.EMPTY_BODY)));
        }
    }
}
