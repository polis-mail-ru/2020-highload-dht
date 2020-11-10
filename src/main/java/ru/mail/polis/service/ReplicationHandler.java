package ru.mail.polis.service;

import com.google.common.collect.ImmutableSet;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;

class ReplicationHandler {

    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);

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
    private final DAO dao;
    @NotNull
    private final Topology topology;
    @NotNull
    private final Map<String, HttpClient> nodesToClients;
    @NotNull
    private final FuturesHandler futuresHandler;

    ReplicationHandler(
            @NotNull final DAO dao,
            @NotNull final Topology topology,
            @NotNull final Map<String, HttpClient> nodesToClients
    ) {
        this.dao = dao;
        this.topology = topology;
        this.nodesToClients = nodesToClients;
        this.futuresHandler = new FuturesHandler(false, dao);
    }

    Response singleGet(@NotNull final ByteBuffer key) {
        try {
            return new Response(Response.ok(Util.toByteArray(dao.get(key))));
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void multipleGet(
            final Set<String> nodes, @NotNull final Request req, final int ack, @NotNull final HttpSession session
    ) {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.size());
        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            if (node.equals(topology.getCurrentNode())) {
                futures.add(futuresHandler.handleLocal(req));
            } else {
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req).GET().build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        multipleFutureGet(session, futures, values, nodes, ack);
    }

    private void multipleFutureGet(
            @NotNull final HttpSession session, @NotNull final List<CompletableFuture<HttpResponse<byte[]>>> futures,
            @NotNull final List<Value> values, final Set<String> nodes, final int ack
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        allOf = allOf.thenAccept((response) -> {
            try {
                session.sendResponse(futuresHandler.futureGet(values, atomicInteger, futures, nodes, ack));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        allOf.exceptionally(response -> {
            try {
                if (futures.size() == 1) {
                    session.sendError(Response.GATEWAY_TIMEOUT, MESSAGE_MAP.get(ErrorNames.TIMEOUT_ERROR));
                } else {
                    session.sendResponse(futuresHandler.futureGet(values, atomicInteger, futures, nodes, ack));
                }
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
            return null;
        });
    }

    Response singleUpsert(
            @NotNull final ByteBuffer key, final byte[] byteVal
    ) {
        final ByteBuffer val = ByteBuffer.wrap(byteVal);
        try {
            dao.upsert(key, val);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void multipleUpsert(
            final Set<String> nodes, @NotNull final Request req,
            final int count, @NotNull final HttpSession session
    ) {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isSelfId(node)) futures.add(futuresHandler.handleLocal(req));
            else {
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(req.getBody()))
                        .build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(futuresHandler.futureUpsert(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(futuresHandler.futureUpsert(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
            return null;
        });
    }

    Response singleDelete(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void multipleDelete(
            final Set<String> nodes, @NotNull final Request req,
            final int count, final HttpSession session
    ) {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isSelfId(node)) futures.add(futuresHandler.handleLocal(req));
            else {
                final HttpRequest request = FuturesHandler.setProxyHeader(node, req).DELETE().build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(futuresHandler.futureDelete(atomicInteger, count, futures));
            } catch (IOException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(futuresHandler.futureDelete(atomicInteger, count, futures));
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

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Set<String> nodes;
        try {
            nodes = ReplicationServiceUtils.getNodeReplica(key, replicationFactor, isForwardedRequest, topology);
        } catch (NotEnoughNodesException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.NOT_ENOUGH_NODES), e);
            return;
        }
        futuresHandler.isProxied = isForwardedRequest;

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
