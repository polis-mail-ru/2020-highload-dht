package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ReplicasNettyRequests {
    private final Map<String, HttpClient> clusterClients;
    private static final String PROXY_HEADER = "X-OK-Proxy";
    private static final String TIMESTAMP_HEADER = "Timestamp";
    private static final HttpResponseStatus CREATED = HttpResponseStatus.CREATED;
    private static final HttpResponseStatus ACCEPTED = HttpResponseStatus.ACCEPTED;
    private static final HttpResponseStatus NOT_FOUND = HttpResponseStatus.NOT_FOUND;
    private static final HttpResponseStatus GATEWAY_TIMEOUT = HttpResponseStatus.GATEWAY_TIMEOUT;
    private static final HttpResponseStatus OK = HttpResponseStatus.OK;

    private final Topology nodes;
    private final ServiceUtils serviceUtils;


    ReplicasNettyRequests(final Topology nodes, final Map<String, HttpClient> clusterClients,
                          final ServiceUtils serviceUtils) {
        this.clusterClients = clusterClients;
        this.nodes = nodes;
        this.serviceUtils = serviceUtils;
    }

    /**
     * Request handler for input GET-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaFactor - input replica factor
     * @param request - input http-request
     */
    public void multiGet(@NotNull final ChannelHandlerContext ctx, @NotNull final Replicas replicaFactor,
                         @NotNull final FullHttpRequest request) {

        final String id = queryParser(request.uri());
        final boolean isForwarded = request.headers().contains(PROXY_HEADER);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<TimestampRecord>> responses = new ArrayList<>(replicaFactor.getFrom());
        Set<String> replicaClusters;

        if (isForwarded) {
            replicaClusters = Collections.singleton(nodes.getId());
        } else {
            replicaClusters = nodes.getReplicas(key, replicaFactor);
        }

        for (final String node : replicaClusters) {
            if (node.equals(nodes.getId())) {
                responses.add(serviceUtils.getTimestampResponse(key));
            } else {
                responses.add(serviceUtils.getProxyResponse(clusterClients, node, id));
            }
        }

        serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, replicaFactor.getAck(), isForwarded)
                .handle((res, ex) -> {
                    if (ex == null) {
                        return processResponses(res);
                    }

                    return ServiceUtils.responseBuilder(GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY);
                }));
    }

    /**
     * Request handler for input PUT-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaFactor - input replica factor
     * @param request - input http-request
     */
    public void multiPut(@NotNull final ChannelHandlerContext ctx, @NotNull final Replicas replicaFactor,
                         @NotNull final FullHttpRequest request) {

        final String id = queryParser(request.uri());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final boolean isForwarded = request.headers().contains(PROXY_HEADER);
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicaFactor.getFrom());
        Set<String> replicaClusters;

        if (isForwarded) {
            replicaClusters = Collections.singleton(nodes.getId());
        } else {
            replicaClusters = nodes.getReplicas(key, replicaFactor);
        }

        for (final String node : replicaClusters) {
            final byte[] body = ServiceUtils.getRequestBody(request.content());

            if (node.equals(nodes.getId())) {
                responses.add(serviceUtils.putTimestampResponse(key, body));
            } else {
                responses.add(serviceUtils.putProxyResponse(clusterClients, node, id, body));
            }
        }

        respond(ctx, request, responses, replicaFactor.getAck(), isForwarded, CREATED);
    }

    /**
     * Request handler for input DELETE-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaFactor - input replica factor
     * @param request - input http-request
     */
    public void multiDelete(@NotNull final ChannelHandlerContext ctx, @NotNull final Replicas replicaFactor,
                            @NotNull final FullHttpRequest request) {

        final String id = queryParser(request.uri());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicaFactor.getFrom());
        final boolean isForwarded = request.headers().contains(PROXY_HEADER);

        Set<String> replicaClusters;

        if (isForwarded) {
            replicaClusters = Collections.singleton(nodes.getId());
        } else {
            replicaClusters = nodes.getReplicas(key, replicaFactor);
        }

        for (final String node : replicaClusters) {
            if (node.equals(nodes.getId())) {
                responses.add(serviceUtils.deleteTimestampResponse(key));
            } else {
                responses.add(serviceUtils.deleteProxyResponse(clusterClients, node, id));
            }
        }

        respond(ctx, request, responses, replicaFactor.getAck(), isForwarded, ACCEPTED);
    }

    /**
     * Joiner input set responses for one.
     *
     * @param responses - input set with responses
     */
    private FullHttpResponse processResponses(@NotNull final Collection<TimestampRecord> responses) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);
        final FullHttpResponse response;

        if (mergedResp.isValue()) {
            response = ServiceUtils.responseBuilder(OK, mergedResp.getValueAsBytes());
        } else {
            response = ServiceUtils.responseBuilder(NOT_FOUND, mergedResp.toBytes());
        }

        response.headers().add(TIMESTAMP_HEADER, mergedResp.getTimestamp());
        return response;
    }

    private String queryParser(final String uri) {
        final QueryStringDecoder decoder = new QueryStringDecoder(uri);

        return decoder.parameters().get("id").get(0);
    }

    private void respond(@NotNull final ChannelHandlerContext ctx, @NotNull final FullHttpRequest request,
                         @NotNull Collection<CompletableFuture<FullHttpResponse>> responses, final int acks,
                         final boolean isForwarded, @NotNull final HttpResponseStatus successStatus) {

        serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, acks, isForwarded)
                .handle((res, ex) -> {
                    if (ex == null) {
                        return ServiceUtils.responseBuilder(successStatus, ServiceUtils.EMPTY_BODY);

                    }

                    return ServiceUtils.responseBuilder(GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY);
                })
        );
    }
}
