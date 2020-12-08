package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ReplicasNettyRequests {
    private final HttpClient client;
    private final Topology nodes;
    private final ServiceUtils serviceUtils;

    ReplicasNettyRequests(@NotNull final Topology nodes, @NotNull final HttpClient client,
                          @NotNull final ServiceUtils serviceUtils) {
        this.client = client;
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
                         @NotNull final FullHttpRequest request, @NotNull final String id) {

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<TimestampRecord>> responses = new ArrayList<>(replicaFactor.getFrom());
        final boolean isForwarded = serviceUtils.isForwarded(request);
        final Set<String> replicaClusters;

        if (isForwarded) {
             replicaClusters = Collections.singleton(nodes.getId());
        } else {
            replicaClusters = nodes.getReplicas(key, replicaFactor);
        }

        for (final String node : replicaClusters) {
            if (node.equals(nodes.getId())) {
                responses.add(serviceUtils.getTimestampResponse(key));
            } else {
                responses.add(serviceUtils.getProxyResponse(client, node, id));
            }
        }

        serviceUtils.respond(ctx, request, serviceUtils.atLeastAsync(responses, replicaFactor.getAck(),
                isForwarded).handle((res, ex) -> {
                    if (ex == null) {
                        return processResponses(res);
                    }
                    return ServiceUtils.responseBuilder(ServiceUtils.GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY);
                })
        );
    }

    /**
     * Request handler for input PUT-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaFactor - input replica factor
     * @param request - input http-request
     */
    public void multiPut(@NotNull final ChannelHandlerContext ctx, @NotNull final Replicas replicaFactor,
                         @NotNull final FullHttpRequest request, @NotNull final String id) {

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicaFactor.getFrom());
        final boolean isForwarded = serviceUtils.isForwarded(request);
        final Set<String> replicaClusters;

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
                responses.add(serviceUtils.putProxyResponse(client, node, id, body));
            }
        }

        respond(ctx, request, responses, replicaFactor.getAck());
    }

    /**
     * Request handler for input DELETE-request with many replicas.
     *
     * @param ctx - http-context
     * @param replicaFactor - input replica factor
     * @param request - input http-request
     */
    public void multiDelete(@NotNull final ChannelHandlerContext ctx, @NotNull final Replicas replicaFactor,
                            @NotNull final FullHttpRequest request, @NotNull final String id) {

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Collection<CompletableFuture<FullHttpResponse>> responses = new ArrayList<>(replicaFactor.getFrom());
        final boolean isForwarded = serviceUtils.isForwarded(request);
        final Set<String> replicaClusters;

        if (isForwarded) {
            replicaClusters = Collections.singleton(nodes.getId());
        } else {
            replicaClusters = nodes.getReplicas(key, replicaFactor);
        }

        for (final String node : replicaClusters) {
            if (node.equals(nodes.getId())) {
                responses.add(serviceUtils.deleteTimestampResponse(key));
            } else {
                responses.add(serviceUtils.deleteProxyResponse(client, node, id));
            }
        }

        respond(ctx, request, responses, replicaFactor.getAck());
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
            response = ServiceUtils.responseBuilder(ServiceUtils.OK, mergedResp.getValueAsBytes());
        } else {
            response = ServiceUtils.responseBuilder(ServiceUtils.NOT_FOUND, mergedResp.toBytes());
        }

        response.headers().add(ServiceUtils.TIMESTAMP_HEADER, mergedResp.getTimestamp());
        return response;
    }

    private void respond(@NotNull final ChannelHandlerContext context, @NotNull final FullHttpRequest request,
                         @NotNull final Collection<CompletableFuture<FullHttpResponse>> responses, final int acks) {

        final HttpResponseStatus successCode;

        if (HttpMethod.PUT.equals(request.method())) {
            successCode = ServiceUtils.CREATED;
        } else {
            successCode = ServiceUtils.ACCEPTED;
        }

        serviceUtils.respond(context, request, serviceUtils.atLeastAsync(responses, acks,
                serviceUtils.isForwarded(request)).handle((res, ex) -> {
                    if (ex == null) {
                        return ServiceUtils.responseBuilder(successCode, ServiceUtils.EMPTY_BODY);
                    }
                    return ServiceUtils.responseBuilder(ServiceUtils.GATEWAY_TIMEOUT, ServiceUtils.EMPTY_BODY);
                })
        );
    }
}
