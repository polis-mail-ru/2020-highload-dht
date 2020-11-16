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
import ru.mail.polis.util.FuturesHandler;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;

class ReplicationHandler {

    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);
    private final ExecutorService exec;
    private final DAO dao;

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
    @NotNull
    private final Map<String, HttpClient> nodesToClients;

    ReplicationHandler(
            @NotNull final DAO dao,
            @NotNull final Topology topology,
            @NotNull final Map<String, HttpClient> nodesToClients,
            @NotNull final ExecutorService exec
    ) {
        this.topology = topology;
        this.nodesToClients = nodesToClients;
        this.exec = exec;
        this.dao = dao;
    }

    private void multipleGet(
            final Set<String> nodes, @NotNull final Request req, final int ack, @NotNull final HttpSession session
    ) {
        final List<CompletableFuture<Value>> futures = new ArrayList<>(nodes.size());
        final List<Value> values = new ArrayList<>();
        final String id = req.getParameter("id");
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
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req).GET().build();
                final CompletableFuture<Value> responses = nodesToClients.get(node)
                        .sendAsync(request, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, exec);
                futures.add(responses);
            }
        }
        multipleFutureGet(session, futures, values, ack);
    }

    private void multipleFutureGet(
            @NotNull final HttpSession session, @NotNull final List<CompletableFuture<Value>> futures,
            @NotNull final List<Value> values, final int ack
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        allOf = allOf.thenAccept((response) -> {
            try {
                session.sendResponse(FuturesHandler.futureGet(values, atomicInteger, futures, ack));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        allOf.exceptionally(response -> {
            try {
                session.sendResponse(FuturesHandler.futureGet(values, atomicInteger, futures, ack));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
            return null;
        });
    }

    private void multipleUpsert(
            final Set<String> nodes, @NotNull final Request req,
            final int count, @NotNull final HttpSession session
    ) {
        final List<CompletableFuture<Response>> futures = new ArrayList<>(nodes.size());
        final String id = req.getParameter("id");
        for (final String node : nodes) {
            if (topology.isSelfId(node)) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.upsertValue(Util.toByteBuffer(id), ByteBuffer.wrap(req.getBody()));
                                return new Response(Response.CREATED, Response.EMPTY);
                            } catch (IOException e) {
                                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                            }
                        }, exec)
                );
            } else {
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(req.getBody()))
                        .build();
                final CompletableFuture<Response> responses = nodesToClients.get(node)
                        .sendAsync(request, PutBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.CREATED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(FuturesHandler.futureUpsert(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(FuturesHandler.futureUpsert(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
            return null;
        });
    }

    private void multipleDelete(
            final Set<String> nodes, @NotNull final Request req,
            final int count, final HttpSession session
    ) {
        final String id = req.getParameter("id");
        final List<CompletableFuture<Response>> futures = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isSelfId(node)) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.removeValue(Util.toByteBuffer(id));
                                return new Response(Response.ACCEPTED, Response.EMPTY);
                            } catch (IOException e) {
                                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                            }
                        }, exec)
                );
            } else {
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req).DELETE().build();
                final CompletableFuture<Response> responses = nodesToClients.get(node)
                        .sendAsync(request, DeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.ACCEPTED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(FuturesHandler.futureDelete(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(FuturesHandler.futureDelete(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
            return null;
        });
    }

    void handle(
            final boolean isForwardedRequest, @NotNull final Request req, @NotNull final HttpSession session,
            final String id, final String replicas
    ) throws IOException {
        final ReplicationFactor replicationFactor;

        try {
            replicationFactor = replicas == null ? ReplicationFactor.getQuorum(topology.getSize()) :
                    ReplicationFactor.createReplicationFactor(replicas);
        } catch (IllegalArgumentException ex) {
            session.sendError(Response.BAD_REQUEST, ex.getMessage());
            return;
        }

        final ByteBuffer key = Util.toByteBuffer(id);
        final Set<String> nodes;
        try {
            nodes = ReplicationServiceUtils.getNodeReplica(key, replicationFactor, isForwardedRequest, topology);
        } catch (NotEnoughNodesException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.NOT_ENOUGH_NODES), e);
            return;
        }

        try {
            switch (req.getMethod()) {
                case Request.METHOD_GET:
                    multipleGet(nodes, req, replicationFactor.getAck(), session);
                    break;
                case Request.METHOD_PUT:
                    multipleUpsert(nodes, req, replicationFactor.getAck(), session);
                    break;
                case Request.METHOD_DELETE:
                    multipleDelete(nodes, req, replicationFactor.getAck(), session);
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, MESSAGE_MAP.get(ErrorNames.NOT_ALLOWED));
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, MESSAGE_MAP.get(ErrorNames.TIMEOUT_ERROR));
        }
    }
}
