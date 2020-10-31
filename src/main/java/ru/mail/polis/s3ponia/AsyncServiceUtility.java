package ru.mail.polis.s3ponia;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.dao.s3ponia.Table;
import ru.mail.polis.service.s3ponia.AsyncService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

import static ru.mail.polis.service.s3ponia.AsyncService.EMPTY;
import static ru.mail.polis.service.s3ponia.AsyncService.logger;

public final class AsyncServiceUtility {

    private AsyncServiceUtility() {
    }

    /**
     * Put implementation for AsyncService.
     *
     * @param id       id parameter in uri
     * @param replicas replicas parameter in uri
     * @param request  user request
     * @param session  user session
     * @param service  handling service
     */
    public static void putImpl(
            @NotNull final String id,
            @NotNull final String replicas,
            @NotNull final Request request,
            @NotNull final HttpSession session,
            @NotNull final AsyncService service) throws IOException {
        try {
            final var key = Utility.byteBufferFromString(id);
            final var value = ByteBuffer.wrap(request.getBody());

            final var header = Utility.Header.getHeader(Utility.TIME_HEADER, request);
            if (header != null) {
                final var time = Long.parseLong(header.value);
                if (upsertWithTimeStamp(key, value, time, service)
                        .whenCompleteAsync((c, t) -> handlingUpsertResult(session, t))
                        .isCancelled()) {
                    logger.error("Cancelled upserting");
                }
                return;
            }

            final Utility.ReplicationConfiguration parsed =
                    getReplicationConfiguration(replicas, session, service);
            if (parsed == null) return;

            final var currTime = System.currentTimeMillis();
            request.addHeader(Utility.TIME_HEADER + ": " + currTime);

            final var nodes = service.policy.getNodeReplicas(key, parsed.from);
            final var responses =
                    getFuturesResponse(id, HttpRequest.newBuilder()
                                    .header(Utility.PROXY_HEADER, service.policy.homeNode())
                                    .headers(Utility.TIME_HEADER, Long.toString(currTime))
                                    .timeout(Duration.ofMillis(100))
                                    .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody())),
                            parsed, service, nodes);
            final boolean homeInReplicas = Utility.isHomeInReplicas(service.policy.homeNode(), nodes);

            if (homeInReplicas) {
                responses.add(upsertWithTimeStamp(key, value, currTime, service));
            }

            if (Utility.atLeast(parsed.ack, responses).whenCompleteAsync(validatePut(session)).isCancelled()) {
                AsyncService.logger.error("Canceled task");
            }
        } catch (RejectedExecutionException e) {
            AsyncService.logger.error("Error", e);
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, EMPTY));
        }
    }

    private static void handlingUpsertResult(@NotNull final HttpSession session,
                                             final Throwable t) {
        try {
            if (t == null) {
                session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
            } else {
                session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
            }
        } catch (IOException e) {
            logger.error("Error in sending put response", e);
        }
    }

    @NotNull
    private static BiConsumer<Collection<Void>, Throwable> validatePut(@NotNull final HttpSession session) {
        return (c, t) -> {
            try {
                if (t == null) {
                    session.sendResponse(new Response(Response.CREATED, EMPTY));
                } else {
                    AsyncService.logger.error("Error in proxying", t);
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                }
            } catch (IOException e) {
                AsyncService.logger.error("Error in sending response in putting", e);
            }
        };
    }

    /**
     * Asynchronous deleting key.
     *
     * @param key      key to delete
     * @param currTime deleting time
     * @param service  service where to delete
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> deleteWithTimeStampAsync(@NotNull final ByteBuffer key,
                                                                   final long currTime,
                                                                   @NotNull final AsyncService service) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                service.dao.removeWithTimeStamp(key, currTime);
                return null;
            } catch (IOException e) {
                AsyncService.logger.error(
                        "IOException in putting key(size: {}) from dao on node {}",
                        key.capacity(), service.policy.homeNode(), e);
                throw new RuntimeException("IOException in putting key", e);
            }
        }, service.es);
    }

    private static CompletableFuture<Void> upsertWithTimeStamp(@NotNull final ByteBuffer key,
                                                               @NotNull final ByteBuffer value,
                                                               final long currTime,
                                                               @NotNull final AsyncService service) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                service.dao.upsertWithTimeStamp(key, value, currTime);
                return null;
            } catch (IOException e) {
                AsyncService.logger.error(
                        "IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                        key.capacity(), value.capacity(), service.policy.homeNode(), e);
                throw new RuntimeException("IOException in putting key", e);
            }
        }, service.es);
    }

    /**
     * Handling error in status.
     *
     * @param session session for response
     */
    public static void handleStatusError(@NotNull final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            AsyncService.logger.error("Error in sending status", e);
        }
    }

    /**
     * Get proxy response.
     *
     * @param request proxying request
     * @param service proxying service
     * @return Response from node
     */
    public static <T> CompletableFuture<T> proxyAsync(
            @NotNull final HttpRequest request,
            @NotNull final AsyncService service,
            @NotNull final HttpResponse.BodyHandler<T> handler) {
        return service.httpClient.sendAsync(request, handler).thenApplyAsync(HttpResponse::body);
    }

    /**
     * Default HttpRequest.Builder for node and id.
     *
     * @param node node for request
     * @param id   param in request
     * @return HttpRequest.Builder
     */
    private static HttpRequest.Builder request(
            @NotNull final String node,
            @NotNull final String id) {
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(node + "/v0/entity?id=" + id))
                    .timeout(Duration.ofSeconds(1));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error in parsing URI", e);
        }
    }

    /**
     * Produce list of responses over proxy(node, request, service).
     *
     * @param id            id for proxy
     * @param configuration replication configuration
     * @param service       AsyncService for proxying
     * @param nodes         dest nodes
     * @return list of responses
     */
    @NotNull
    public static Collection<CompletableFuture<Table.Value>> getGetFutures(
            @NotNull final String id,
            @NotNull final Utility.ReplicationConfiguration configuration,
            @NotNull final AsyncService service,
            @NotNull final String... nodes) {
        final List<CompletableFuture<Table.Value>> futureResponses = new ArrayList<>(configuration.from);

        for (final var node :
                nodes) {

            if (!node.equals(service.policy.homeNode())) {
                final var response = proxyAsync(
                        request(node, id).header(Utility.PROXY_HEADER, node).GET().build(), service,
                        getBodyHandler());
                futureResponses.add(response);
            }
        }
        return futureResponses;
    }

    @NotNull
    private static HttpResponse.BodyHandler<Table.Value> getBodyHandler() {
        return responseInfo -> {
            if (responseInfo.statusCode() != 200 /* OK */
                    && responseInfo.statusCode() != 404 /* NOT FOUND */) {
                throw new IllegalArgumentException("Error in get request");
            }

            if (responseInfo.statusCode() == 404 /* NOT FOUND */) {
                return HttpResponse.BodySubscribers.replacing(Table.Value.ABSENT);
            }

            final var header = responseInfo.headers().firstValue(Utility.DEADFLAG_TIMESTAMP_HEADER);
            if (header.isEmpty()) {
                throw new IllegalArgumentException("No timestamp header");
            }
            final var deadFlagTimestamp = Long.parseLong(header.get());

            return HttpResponse.BodySubscribers.mapping(
                    HttpResponse.BodySubscribers.ofByteArray(),
                    (b) -> {
                        final var bBuffer = ByteBuffer.wrap(b);
                        return Table.Value.of(bBuffer, deadFlagTimestamp, -1);
                    }
            );
        };
    }

    /**
     * Produce list of responses over proxy(node, request, service).
     *
     * @param id            id for proxy
     * @param configuration replication configuration
     * @param service       AsyncService for proxying
     * @param nodes         dest nodes
     * @return list of responses
     */
    @NotNull
    public static Collection<CompletableFuture<Void>> getFuturesResponse(
            @NotNull final String id,
            @NotNull final HttpRequest.Builder req,
            @NotNull final Utility.ReplicationConfiguration configuration,
            @NotNull final AsyncService service,
            @NotNull final String... nodes) {
        final List<CompletableFuture<Void>> futureResponses = new ArrayList<>(configuration.from);

        for (final var node :
                nodes) {

            if (!node.equals(service.policy.homeNode())) {
                final CompletableFuture<Void> response =
                        AsyncServiceUtility.proxyAsync(
                                req.uri(URI.create(node + "/v0/entity?id=" + id))
                                        .build(),
                                service,
                                responseInfo -> {
                                    if (responseInfo.statusCode() == 202 /* ACCEPTED */
                                            || responseInfo.statusCode() == 201 /* CREATED */) {
                                        return HttpResponse.BodySubscribers.discarding();
                                    }
                                    throw new IllegalArgumentException("Failure in putting/deleting");
                                });
                futureResponses.add(response);
            }
        }
        return futureResponses;
    }

    /**
     * Parsing replicas.
     *
     * @param replicas string for parsing
     * @param service  AsyncService with nodes
     * @return replication configuration
     */
    private static Utility.ReplicationConfiguration parseAndValidateReplicas(final String replicas,
                                                                             @NotNull final AsyncService service) {
        final Utility.ReplicationConfiguration parsedReplica;
        final var nodeCount = service.policy.all().length;

        parsedReplica = replicas == null ? AsyncService.DEFAULT_CONFIGURATIONS.get(nodeCount - 1) :
                Utility.ReplicationConfiguration.parse(replicas);

        if (parsedReplica == null || parsedReplica.ack <= 0
                || parsedReplica.ack > parsedReplica.from || parsedReplica.from > nodeCount) {
            return null;
        }

        return parsedReplica;
    }

    /**
     * Combine parseAndValidateReplicas and handling error in 1 step.
     *
     * @param replicas string for replicas parsing
     * @param session  HttpSession for sending responses
     * @param service  AsyncService for parseAndValidateReplicas
     * @return replication configuration
     * @throws IOException rethrow IOException from session
     */
    @Nullable
    public static Utility.ReplicationConfiguration getReplicationConfiguration(
            @NotNull final String replicas,
            @NotNull final HttpSession session,
            @NotNull final AsyncService service) throws IOException {
        final var parsed = parseAndValidateReplicas(replicas, service);

        if (parsed == null) {
            AsyncService.logger.error("Bad replicas param {}", replicas);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return null;
        }
        return parsed;
    }
}
