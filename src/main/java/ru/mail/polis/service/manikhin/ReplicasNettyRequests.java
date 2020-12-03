package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

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
    private final ServiceUtils serviceUtils;
    private final ThreadPoolExecutor executor;

    ReplicasNettyRequests(final DAO dao, final Topology nodes, final Map<String, HttpClient> clusterClients,
                          final ThreadPoolExecutor executor, final int timeout) {
        this.clusterClients = clusterClients;
        this.nodes = nodes;
        this.executor = executor;
        this.serviceUtils = new ServiceUtils(dao, executor, timeout);
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
            switch (request.method().toString()) {
                case "GET":
                    multiGet(ctx, replicaClusters, request, replicateAcks);
                    break;
                case "PUT":
                    multiPut(ctx, replicaClusters, request, replicateAcks);
                    break;
                case "DELETE":
                    multiDelete(ctx, replicaClusters, request, replicateAcks);
                    break;
                default:
                    ServiceUtils.sendResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, ServiceUtils.EMPTY_BODY,
                            ctx, request);
                    break;
            }
        } catch (IllegalStateException error) {
            log.error("handleMultiRequest error: ", error);
            ServiceUtils.sendResponse(HttpResponseStatus.GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY, ctx, request);
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
                responses.add(serviceUtils.getTimestampResponse(key));
            } else {
                responses.add(serviceUtils.getProxyResponse(clusterClients, node, id));
            }

            serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                    .handle((res, ex) -> ex == null ? processResponses(res) : ServiceUtils.responseBuilder(
                            HttpResponseStatus.GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY)
                    )
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
                responses.add(serviceUtils.putTimestampResponse(key, ServiceUtils.getRequestBody(request.content())));
            } else {
                final byte[] body = ServiceUtils.getRequestBody(request.content());
                responses.add(serviceUtils.putProxyResponse(clusterClients, node, id, body));
            }
        }

        serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                .thenApplyAsync(res -> ServiceUtils.responseBuilder(HttpResponseStatus.CREATED,
                        ServiceUtils.EMPTY_BODY), executor)
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
                responses.add(serviceUtils.deleteTimestampResponse(key));
            } else {
                responses.add(serviceUtils.deleteProxyResponse(clusterClients, node, id));
            }
        }

        serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, replicateAcks, isForwardedRequest)
                .thenApplyAsync(res -> ServiceUtils.responseBuilder(HttpResponseStatus.ACCEPTED,
                        ServiceUtils.EMPTY_BODY), executor)
        );
    }

    /**
     * Joiner input set responses for one.
     *
     * @param responses - input set with responses
     */
    public FullHttpResponse processResponses(@NotNull final Collection<TimestampRecord> responses) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);
        final FullHttpResponse response;

        if (mergedResp.isValue()) {
            response = ServiceUtils.responseBuilder(HttpResponseStatus.OK, mergedResp.getValueAsBytes());
        } else {
            response = ServiceUtils.responseBuilder(HttpResponseStatus.NOT_FOUND, mergedResp.toBytes());
        }

        response.headers().add("Timestamp", mergedResp.getTimestamp());
        return response;
    }

    private String queryParser(final String uri) {
        final QueryStringDecoder decoder = new QueryStringDecoder(uri);

        return decoder.parameters().get("id").get(0);
    }
}
