package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.TimestampRecord;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

public class ReplicasNettyRequests {
    private final Map<String, HttpClient> clusterClients;
    private final Logger log = LoggerFactory.getLogger(ReplicasNettyRequests.class);
    private static final String PROXY_HEADER = "X-OK-Proxy";
    private final Topology nodes;
    private final Utils utils;
    private final ThreadPoolExecutor executor;

    ReplicasNettyRequests(final DAO dao, final Topology nodes, final Map<String, HttpClient> clusterClients,
                          final ThreadPoolExecutor executor, final int timeout) {
        this.clusterClients = clusterClients;
        this.nodes = nodes;
        this.executor = executor;
        this.utils = new Utils(dao, executor, timeout);
    }

    /**
     * Request handler for input requests with many replicas.
     *
     * @param replicaClusters - replica clusters
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     * @param ctx - http-session
     */
    public void handleMultiRequest(@NotNull final Set<String> replicaClusters, @NotNull final FullHttpRequest request,
                                   final int replicateAcks, @NotNull final ChannelHandlerContext ctx) {
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
                Utils.sendResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, Utils.EMPTY_BODY, ctx, request);
                return;
            }
        } catch (IllegalStateException error) {
            log.error("handleMultiRequest error: ", error);
            Utils.sendResponse(HttpResponseStatus.GATEWAY_TIMEOUT, Utils.EMPTY_BODY, ctx, request);
            return;
        }
    }

    /**
     * Request handler for input GET-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiGet(@NotNull final ChannelHandlerContext ctx, @NotNull final Set<String> replicaNodes,
                         @NotNull final FullHttpRequest request, final int replicateAcks) {

        final String id = queryParser(request.uri());
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<TimestampRecord>> responses = new ArrayList<>();

        for (final String node : replicaNodes) {
            if (node.equals(nodes.getId())) {
                responses.add(utils.getResponse(key));
            } else {
                responses.add(utils.getProxyResponse(clusterClients, node, id));
            }

            utils.respond(ctx, request, utils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                    .thenApplyAsync(res -> processResponses(res, isForwardedRequest))
            );
        }
    }

    /**
     * Request handler for input PUT-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiPut(@NotNull final ChannelHandlerContext ctx, @NotNull final Set<String> replicaNodes,
                         @NotNull final FullHttpRequest request, final int replicateAcks) {

        final String id = queryParser(request.uri());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicateAcks);

        for (final String node : replicaNodes) {
            if (node.equals(nodes.getId())) {
                responses.add(utils.putResponse(key, Utils.getRequestBody(request.content())));
            } else {
                final byte[] body = Utils.getRequestBody(request.content());
                responses.add(utils.putProxyResponse(clusterClients, node, id, body));
            }
        }

        utils.respond(ctx, request, utils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                .thenApplyAsync(res -> Utils.responseBuilder(HttpResponseStatus.CREATED, Utils.EMPTY_BODY), executor)
        );
    }

    /**
     * Request handler for input DELETE-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiDelete(@NotNull final ChannelHandlerContext ctx, @NotNull final Set<String> replicaNodes,
                            @NotNull final FullHttpRequest request, final int replicateAcks) {

        final String id = queryParser(request.uri());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicateAcks);
        final boolean isForwardedRequest = request.headers().contains(PROXY_HEADER);

        for (final String node : replicaNodes) {
            if (node.equals(nodes.getId())) {
                responses.add(utils.deleteResponse(key));
            } else {
                responses.add(utils.deleteProxyResponse(clusterClients, node, id));
            }
        }

        utils.respond(ctx, request, utils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                .thenApplyAsync(res -> Utils.responseBuilder(HttpResponseStatus.ACCEPTED, Utils.EMPTY_BODY),
                        executor)
        );
    }

    /**
     * Joiner input set responses for one.
     *
     * @param responses - input set with responses
     * @param isForwardedRequest - check result request on forwarding
     */
    public FullHttpResponse processResponses(@NotNull final Collection<TimestampRecord> responses,
                                             final boolean isForwardedRequest) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);

        if (mergedResp.isValue()) {
            if (isForwardedRequest) {
                return Utils.responseBuilder(HttpResponseStatus.OK, mergedResp.toBytes());
            } else {
                return Utils.responseBuilder(HttpResponseStatus.OK, mergedResp.getValueAsBytes());
            }
        } else {
            return Utils.responseBuilder(HttpResponseStatus.NOT_FOUND, mergedResp.toBytes());
        }
    }

    private String queryParser(final String uri) {
        final QueryStringDecoder decoder = new QueryStringDecoder(uri);

        return decoder.parameters().get("id").get(0);
    }
}
