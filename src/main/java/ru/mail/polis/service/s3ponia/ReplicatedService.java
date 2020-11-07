package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpClient;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.Proxy;
import ru.mail.polis.util.Utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ReplicatedService implements HttpEntityHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReplicatedService.class);
    final AsyncService asyncService;
    final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;

    /**
     * Creates a new {@link ReplicatedService} with given {@link AsyncService} and {@link ShardingPolicy}.
     *
     * @param service {@link HttpEntityHandler} base service for proxy handle
     * @param policy  {@link ShardingPolicy} replica policy
     */
    public ReplicatedService(@NotNull final AsyncService service,
                             @NotNull final ShardingPolicy<ByteBuffer, String> policy) {
        this.asyncService = service;
        this.policy = policy;
        this.urlToClient = Utility.urltoClientFromSet(policy.homeNode(), policy.all());
    }

    @Override
    public void entity(@NotNull final String id,
                       final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        final var proxyHeader = Header.getHeader(Proxy.PROXY_HEADER, request);
        if (proxyHeader != null) {
            asyncService.entity(id, replicas, request, session);
            return;
        }
        final var time = System.currentTimeMillis();
        request.addHeader(Utility.TIME_HEADER + ": " + time);
        final ByteBuffer value;
        if (request.getBody() == null) {
            value = ByteBuffer.allocate(0);
        } else {
            value = ByteBuffer.wrap(request.getBody());
        }

        final ReplicationConfiguration parsedReplica;
        try {
            parsedReplica = ReplicationConfiguration.parseOrDefault(replicas, policy.all().length);
        } catch (IllegalArgumentException e) {
            logger.error("Bad request", e);
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new RuntimeException("Bad request's method", e);
        }

        final var key = Utility.byteBufferFromString(id);
        final var nodes = policy.getNodeReplicas(key, parsedReplica.replicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);

        final var replicaResponses =
                Proxy.proxyReplicas(request, urlToClient.values()
                        .stream()
                        .limit(parsedReplica.replicas)
                        .collect(Collectors.toList()));

        final Response resultResponse;

        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    resultResponse = resolveGetProxyResult(
                            parsedReplica, homeInReplicas, replicaResponses, key);
                    break;
                }
                case Request.METHOD_DELETE: {
                    resultResponse =
                            resolveDeleteProxyResult(parsedReplica, homeInReplicas, replicaResponses, key, time);
                    break;
                }
                case Request.METHOD_PUT: {
                    resultResponse =
                            resolvePutProxyResult(parsedReplica, homeInReplicas, replicaResponses, key, value, time);
                    break;
                }
                default: {
                    logger.error("Unhandled request method");
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    return;
                }
            }
            session.sendResponse(resultResponse);
        } catch (ReplicaException e) {
            logger.error("Not enough replicas response", e);
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    private static Response fromFutureResponse(CompletableFuture<Response> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            logger.error("Error in getting future", e);
            throw new FutureResponseException("Error in getting from future");
        }
    }

    private Response resolveGetProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                           final boolean homeInReplicas,
                                           @NotNull final List<Response> replicaResponses,
                                           @NotNull final ByteBuffer key) throws ReplicaException {
        if (homeInReplicas) {
            try {
                replicaResponses.add(fromFutureResponse(asyncService.getAsync(key)));
            } catch (FutureResponseException ignored) {
                logger.error("Error in getting from dao");
            }
        }
        final List<Value> values;
        try {
            values = replicaResponses.stream()
                    .map(Value::fromResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid response for parsing to value", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        if (values.size() < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas in getting");
        }

        values.sort(Value.valueResponseComparator());

        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            return Response.ok(Utility.fromByteBuffer(bestVal.getValue()));
        }
    }

    private Response resolvePutProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                           final boolean homeInReplicas,
                                           @NotNull final List<Response> replicaResponses,
                                           @NotNull final ByteBuffer key,
                                           @NotNull final ByteBuffer value,
                                           final long time) throws ReplicaException {
        if (homeInReplicas) {
            try {
                replicaResponses.add(fromFutureResponse(asyncService.putAsync(key, value, time)));
            } catch (FutureResponseException e) {
                logger.error("Error in deleting from dao", e);
            }
        }
        if (replicaResponses.size() < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas in putting");
        } else {
            return new Response(Response.CREATED, Response.EMPTY);
        }
    }

    private Response resolveDeleteProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                              final boolean homeInReplicas,
                                              @NotNull final List<Response> replicaResponses,
                                              @NotNull final ByteBuffer key,
                                              final long time)
            throws ReplicaException {
        if (homeInReplicas) {
            try {
                replicaResponses.add(fromFutureResponse(asyncService.deleteAsync(key, time)));
            } catch (FutureResponseException e) {
                logger.error("Error in deleting from dao", e);
            }
        }
        if (replicaResponses.size() < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas in deleting");
        } else {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }
    }

    @Override
    public void close() throws IOException {
        asyncService.close();
    }
}
