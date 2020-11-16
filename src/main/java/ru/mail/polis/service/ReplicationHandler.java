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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;
import static ru.mail.polis.service.ReplicationServiceUtils.syncValues;

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
            @NotNull final DAO dao, @NotNull final Topology topology,
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
        final List<Value> values = new ArrayList<>(nodes.size());
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
                final HttpRequest request = Util.setProxyHeader(node, req).GET().build();
                final CompletableFuture<Value> responses = nodesToClients.get(node)
                        .sendAsync(request, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, exec);
                futures.add(responses);
            }
        }
        try {
            session.sendResponse(futureGet(values, futures, ack));
        } catch (IOException exc) {
            log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR));
        }
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
                final HttpRequest request = Util.setProxyHeader(node, req)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(req.getBody()))
                        .build();
                final CompletableFuture<Response> responses = nodesToClients.get(node)
                        .sendAsync(request, PutBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.CREATED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }
        try {
            session.sendResponse(futureHelper(count, futures, Response.CREATED, 201));
        } catch (IOException exc) {
            log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR));
        }
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
                final HttpRequest request = Util.setProxyHeader(node, req).DELETE().build();
                final CompletableFuture<Response> responses = nodesToClients.get(node)
                        .sendAsync(request, DeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> new Response(Response.ACCEPTED, Response.EMPTY), exec);
                futures.add(responses);
            }
        }
        try {
            session.sendResponse(futureHelper(count, futures, Response.ACCEPTED, 202));
        } catch (IOException exc) {
            log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR));
        }
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

    /**
     * @param ack          - ack target value
     * @param futures      - futures list
     * @param responseCode - target response code
     * @return - one-nio Response
     */
    private static Response futureHelper(
            final int ack,
            final List<CompletableFuture<Response>> futures,
            final String responseCode,
            final int code
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final boolean res = count(ack, atomicInteger, code, futures);

        if (res) {
            return new Response(responseCode, Response.EMPTY);
        }
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    private static boolean count(
            final int ack,
            final AtomicInteger atomicInteger,
            final int returnCode,
            final List<CompletableFuture<Response>> futures
    ) {
        for (final CompletableFuture<Response> future : futures) {
            try {
                final Response result = future.get();
                if (result.getStatus() == returnCode) {
                    atomicInteger.incrementAndGet();
                    if (atomicInteger.get() == futures.size() || atomicInteger.get() == ack) {
                        return true;
                    }
                }
            } catch (ExecutionException | InterruptedException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), exc);
            }
        }
        return false;
    }

    private static Response futureGet(
            final List<Value> values,
            final List<CompletableFuture<Value>> futures,
            final int ack
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        for (final CompletableFuture<Value> future : futures) {
            try {
                if (future.isCompletedExceptionally()) continue;
                values.add(future.get());
                atomicInteger.incrementAndGet();
                if (atomicInteger.get() == futures.size() || atomicInteger.get() == ack) {
                    return Value.toResponse(syncValues(values));
                }
            } catch (ExecutionException | InterruptedException ex) {
                log.error(MESSAGE_MAP.get(ErrorNames.FUTURE_ERROR), ex);
            }
        }
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }
}
