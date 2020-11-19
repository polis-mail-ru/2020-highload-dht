package ru.mail.polis.service;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.handlers.DeleteBodyHandler;
import ru.mail.polis.service.handlers.GetBodyHandler;
import ru.mail.polis.service.handlers.PutBodyHandler;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static java.util.Map.entry;
import static ru.mail.polis.service.Value.mergeValues;

class ReplicationHandler {
    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);
    private final ExecutorService exec;
    private final DAO dao;
    private final HttpClient httpClient;
    private final ReplicationFactor quorum;

    private enum ErrorNames {
        NOT_FOUND_ERROR, IO_ERROR, QUEUE_LIMIT_ERROR, PROXY_ERROR, NOT_ENOUGH_NODES, TIMEOUT_ERROR,
        NOT_ALLOWED, FUTURE_ERROR
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.NOT_FOUND_ERROR, "Value not found"),
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.QUEUE_LIMIT_ERROR, "Queue is full"),
            entry(ErrorNames.NOT_ENOUGH_NODES, "Not enough nodes in cluster"),
            entry(ErrorNames.PROXY_ERROR, "Error forwarding request via proxy"),
            entry(ErrorNames.TIMEOUT_ERROR, "Your request failed due to timeout"),
            entry(ErrorNames.NOT_ALLOWED, "Method not allowed"),
            entry(ErrorNames.FUTURE_ERROR, "Error handling future")
    );

    @NotNull
    private final Topology topology;

    ReplicationHandler(
            @NotNull final DAO dao, @NotNull final Topology topology,
            @NotNull final ExecutorService exec, @NotNull final HttpClient client
    ) {
        this.topology = topology;
        this.exec = exec;
        this.dao = dao;
        this.httpClient = client;
        this.quorum = ReplicationFactor.getQuorum(topology.getSize());
    }

    private CompletableFuture<Response> multipleGet(
            final Set<String> nodes, final int minSuccesses, final String id, final Request request
    ) {
        final List<CompletableFuture<Value>> futures = new ArrayList<>(nodes.size());
        final ByteBuffer key = Util.toByteBuffer(id);

        for (final String node : nodes) {
            if (node.equals(topology.getCurrentNode())) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return dao.getValue(key);
                            } catch (IOException e) {
                                return Value.resolveMissingValue();
                            }
                        }, exec));
            } else {
                final HttpRequest httpRequest = Util.setProxyHeader(node, request).GET().build();
                final CompletableFuture<Value> responses = httpClient
                        .sendAsync(httpRequest, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, exec);
                futures.add(responses);
            }
        }

        return Util.getMinimumAckData(futures, minSuccesses, exec)
                .handle((res, ex) -> ex == null ? Value.toResponse(mergeValues(res))
                        : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
    }

    private CompletableFuture<Response> multipleUpsert(
            final Set<String> nodes, final int minSuccesses, final String id, @NotNull final Request request
    ) {
        final List<CompletableFuture<Response>> futures = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isSelfId(node)) {
                futures.add(handleLocal(id, request));
            } else {
                final HttpRequest httpRequest = Util.setProxyHeader(node, request)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
                final CompletableFuture<Response> responses = httpClient
                        .sendAsync(httpRequest, PutBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.CREATED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }

        return futureHelper(futures, minSuccesses);
    }

    private CompletableFuture<Response> futureHelper(
            @NotNull final Collection<CompletableFuture<Response>> futures, final int minSuccesses
    ) {
        return Util.getMinimumAckData(futures, minSuccesses, exec)
                .thenApplyAsync((result) -> result.stream()
                        .findFirst()
                        .orElse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY)), exec);
    }

    private CompletableFuture<Response> multipleDelete(
            final Set<String> nodes, final int minSuccesses, final String id, final Request request
    ) {
        final List<CompletableFuture<Response>> futures = new ArrayList<>(nodes.size());

        for (final String node : nodes) {
            if (topology.isSelfId(node)) {
                futures.add(handleLocal(id, request));
            } else {
                final HttpRequest httpRequest = Util.setProxyHeader(node, request).DELETE().build();
                final CompletableFuture<Response> responses = httpClient
                        .sendAsync(httpRequest, DeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.ACCEPTED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }

        return futureHelper(futures, minSuccesses);
    }

    void handle(
            @NotNull final Request request, @NotNull final HttpSession session, final String id, final String replicas
    ) throws IOException {
        final ReplicationFactor replicationFactor;
        try {
            replicationFactor = replicas == null ? quorum :
                    ReplicationFactor.createReplicationFactor(replicas);
        } catch (IllegalArgumentException ex) {
            session.sendError(Response.BAD_REQUEST, ex.getMessage());
            return;
        }

        final boolean isForwardedRequest = request.getHeader(Util.PROXY_HEADER) != null;

        if (isForwardedRequest) {
            sendFromFuture(session, handleLocal(id, request));
            return;
        }

        final ByteBuffer key = Util.toByteBuffer(id);
        final Set<String> nodes;

        try {
            nodes = topology.getReplicas(key, replicationFactor.getFrom());
        } catch (NotEnoughNodesException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.NOT_ENOUGH_NODES), e);
            return;
        }

        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    sendFromFuture(session, multipleGet(nodes, replicationFactor.getAck(), id, request));
                    break;
                case Request.METHOD_PUT:
                    sendFromFuture(session, multipleUpsert(nodes, replicationFactor.getAck(), id, request));
                    break;
                case Request.METHOD_DELETE:
                    sendFromFuture(session, multipleDelete(nodes, replicationFactor.getAck(), id, request));
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, MESSAGE_MAP.get(ErrorNames.NOT_ALLOWED));
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, MESSAGE_MAP.get(ErrorNames.TIMEOUT_ERROR));
        }
    }

    private void trySend(@NotNull final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), e);
        }
    }

    private void sendFromFuture(@NotNull final HttpSession session, @NotNull final CompletableFuture<Response> future) {
        future.whenComplete((r, t) -> {
            if (t == null) {
                trySend(session, r);
            } else {
                if (t instanceof CompletionException) {
                    t = t.getCause();
                }
                final String errorCode = t instanceof IllegalStateException ?
                        Response.GATEWAY_TIMEOUT : Response.INTERNAL_ERROR;
                trySend(session, new Response(errorCode, t.getMessage().getBytes(StandardCharsets.UTF_8)));
            }
        }).isCancelled();
    }

    private CompletableFuture<Response> handleLocal(final String id, final Request request) {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return Value.toResponse(dao.getValue(Util.toByteBuffer(id)));
                            } catch (IOException e) {
                                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                            }
                        }, exec);
            case Request.METHOD_PUT:
                return CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.upsertValue(Util.toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
                                return new Response(Response.CREATED, Response.EMPTY);
                            } catch (IOException e) {
                                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                            }
                        }, exec);
            case Request.METHOD_DELETE:
                return CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.removeValue(Util.toByteBuffer(id));
                                return new Response(Response.ACCEPTED, Response.EMPTY);
                            } catch (IOException e) {
                                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                            }
                        }, exec);
            default:
                return CompletableFuture.supplyAsync(
                        () -> new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY), exec);
        }
    }
}
