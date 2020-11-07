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
import java.util.stream.Collectors;

public class ReplicatedService implements HttpEntityHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReplicatedService.class);
    final AsyncService asyncService;
    final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;

    public ReplicatedService(@NotNull final AsyncService service,
                             ShardingPolicy<ByteBuffer, String> policy) {
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

        final var key = Utility.byteBufferFromString(id);
        final ReplicationConfiguration parsedReplica;
        try {
            parsedReplica = ReplicationConfiguration.parseOrDefault(replicas, policy.all().length);
        } catch (IllegalArgumentException e) {
            logger.error("Bad request", e);
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new RuntimeException("Bad request's method", e);
        }

        final var nodes = policy.getNodeReplicas(key, parsedReplica.replicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);

        final var replicaResponses =
                Proxy.proxyReplicas(request, urlToClient.values());

        final Response resultResponse;

        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    resultResponse = resolveGetProxyResult(
                            parsedReplica, homeInReplicas, replicaResponses, key);
                    break;
                }
                case Request.METHOD_DELETE: {
                    try {
                        replicaResponses.add(asyncService.delete(key, time));
                    } catch (DaoOperationException e) {
                        logger.error("Error in deleting from dao", e);
                    }
                    resultResponse = resolveDeleteProxyResult(parsedReplica, replicaResponses.size());
                    break;
                }
                case Request.METHOD_PUT: {
                    try {
                        replicaResponses.add(asyncService.put(key, value, time));
                    } catch (DaoOperationException e) {
                        logger.error("Error in deleting from dao", e);
                    }
                    resultResponse = resolvePutProxyResult(parsedReplica, replicaResponses.size());
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
            logger.error("Not enough replicas response");
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    private Response resolveGetProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                           final boolean homeInReplicas,
                                           @NotNull final List<Response> replicaResponses,
                                           @NotNull final ByteBuffer key) throws ReplicaException {
        if (homeInReplicas) {
            try {
                replicaResponses.add(asyncService.get(key));
            } catch (DaoOperationException ignored) {
                logger.error("Error in getting from dao");
            }
        }
        final var values = replicaResponses.stream().map(Value::fromResponse).collect(Collectors.toList());
        if (values.size() < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas");
        }

        values.sort(Value.valueResponseComparator());

        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            return Response.ok(Utility.fromByteBuffer(bestVal.getValue()));
        }
    }

    private static Response resolvePutProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                                  final int counter) throws ReplicaException {
        if (counter < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas");
        } else {
            return new Response(Response.CREATED, Response.EMPTY);
        }
    }

    private static Response resolveDeleteProxyResult(@NotNull final ReplicationConfiguration parsedReplica,
                                                     final int counter) throws ReplicaException {
        if (counter < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas");
        } else {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }
    }

    @Override
    public void close() {
        asyncService.close();
    }
}
