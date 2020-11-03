package ru.mail.polis.service.codearound;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  class to feature topology-bound implementations of project-required DAO methods (get, put, delete).
 */
public class ReplicationLsm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationLsm.class);
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    public static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    public static final String GATEWAY_TIMEOUT_ERROR_LOG = "Sending response takes too long. "
            + "Request failed as gateway closed past timeout";
    public static final String IO_ERROR_LOG = "IO exception raised";
    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodesToClients;
    @NotNull
    private final ReplicationFactor repliFactor;
    @NotNull
    private final FutureUtils futUtils;

    /**
     * class const.
     *
     * @param dao - DAO instance
     * @param topology - topology implementation instance
     * @param nodesToClients - HashMap-implemented mapping available nodes over HTTP clients
     * @param repliFactor - replication factor
     */
    ReplicationLsm(@NotNull final DAO dao,
                   @NotNull final Topology<String> topology,
                   @NotNull final Map<String, HttpClient> nodesToClients,
                   @NotNull final ReplicationFactor repliFactor) {
        this.dao = dao;
        this.topology = topology;
        this.nodesToClients = nodesToClients;
        this.repliFactor = repliFactor;
        this.futUtils = new FutureUtils(false, dao);
    }

    /**
     * GET handler applicable for single node topology.
     *
     * @param key - key searched
     * @return HTTP response
     */
    Response getWithOnlyNode(@NotNull final ByteBuffer key) {

        ByteBuffer buf;
        try {
            buf = dao.get(key);
            return new Response(Response.ok(DAOByteOnlyConverter.readByteArray(buf)));
        } catch (NoSuchElementException exc) {
            LOGGER.info(NOT_FOUND_ERROR_LOG);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
            return new Response(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        } catch (IOException exc) {
            LOGGER.error(IO_ERROR_LOG);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * GET handler to run async processing in multi-node cluster.
     *
     * @param nodes - array of node IDs the cluster is build upon
     * @param req - HTTP request
     * @param ack - number of nodes (quorum) to issue success response when processing over replicas
     * @param session - ongoing HTTP session instance
     */
    private void getWithMultipleNodes(final String[] nodes,
                                     @NotNull final Request req,
                                     final int ack,
                                     @NotNull final HttpSession session) throws IOException {

        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            if (node.equals(topology.getThisNode())) {
                futures.add(futUtils.execLocalRequest(req));
            } else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req).GET().build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        if (futures.isEmpty()) {
            session.sendResponse(RepliServiceUtils.processResponses(nodes, values, futUtils.isForwardedRequest()));
            return;
        }
        final AtomicInteger asks = new AtomicInteger(0);
        var all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(futUtils.execGetWithFutures(values, asks, futures, nodes, ack));
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.GET_COMPLETION_ERROR_LOG);
            }
        });
        all.exceptionally(response -> {
            try {
                if (futures.size() == 1) {
                    session.sendError(Response.GATEWAY_TIMEOUT, FutureUtils.GET_COMPLETION_ERROR_LOG);
                } else {
                    session.sendResponse(futUtils.execGetWithFutures(values, asks, futures, nodes, ack));
                }
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.GET_COMPLETION_ERROR_LOG);
            }
            return null;
        });
    }

    /**
     * PUT handler impl for single node topology.
     *
     * @param key - target key
     * @param byteVal - byte array processed as a key-bound value
     * @return HTTP response
     */
    Response upsertWithOnlyNode(
            @NotNull final ByteBuffer key,
            final byte[] byteVal) {

        final ByteBuffer val = ByteBuffer.wrap(byteVal);
        try {
            dao.upsert(key, val);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
            return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error(IO_ERROR_LOG);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * PUT handler to run async processing in multi-node cluster.
     *
     * @param nodes - array of node IDs the cluster is build upon
     * @param req - HTTP request
     * @param count - number of nodes (quorum) to issue success response when processing over replicas
     * @param session - ongoing HTTP session instance
     */
    private void upsertWithMultipleNodes(final String[] nodes,
                                         @NotNull final Request req,
                                         final int count,
                                         @NotNull final HttpSession session) throws IOException {

        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        for (final String node : nodes) {
            if (topology.isThisNode(node)) futures.add(futUtils.execLocalRequest(req));
            else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(req.getBody()))
                        .build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        if (futures.isEmpty()) {
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            return;
        }
        final AtomicInteger quant = new AtomicInteger(0);
        var all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(futUtils.execUpsertWithFutures(quant, count, futures));
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.UPSERT_COMPLETION_ERROR_LOG);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(futUtils.execUpsertWithFutures(quant, count, futures));
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.UPSERT_COMPLETION_ERROR_LOG);
            }
            return null;
        });
    }

    /**
     * DELETE handler impl for single node topology.
     *
     * @param key - target key
     * @return HTTP response
     */
    Response deleteWithOnlyNode(
            @NotNull final ByteBuffer key) {

        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
            return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error(IO_ERROR_LOG);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * DELETE handler to run async processing in multi-node cluster.
     *
     * @param nodes - array of node IDs the cluster is build upon
     * @param req - HTTP request
     * @param count - number of nodes (quorum) to issue success response when processing over replicas
     * @param session - ongoing HTTP session instance
     */
    private void deleteWithMultipleNodes(final String[] nodes,
                                         @NotNull final Request req,
                                         final int count,
                                         final HttpSession session) throws IOException {

        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        for (final String node : nodes) {
            if (topology.isThisNode(node)) futures.add(futUtils.execLocalRequest(req));
            else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req).DELETE().build();
                final CompletableFuture<HttpResponse<byte[]>> responses = nodesToClients.get(node)
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }
        if (futures.isEmpty()) {
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            return;
        }
        final AtomicInteger quant = new AtomicInteger(0);
        var all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        all = all.thenAccept((response) -> {
            try {
                session.sendResponse(futUtils.execDeleteWithFutures(quant, count, futures));
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.DELETE_COMPLETION_ERROR_LOG);
            }
        });
        all.exceptionally(response -> {
            try {
                session.sendResponse(futUtils.execDeleteWithFutures(quant, count, futures));
            } catch (IOException exc) {
                LOGGER.error(FutureUtils.DELETE_COMPLETION_ERROR_LOG);
            }
            return null;
        });
    }

    /**
     * coordinator for method-specific handlers to manage consistent processing, response as well.
     *
     * @param isForwardedRequest - true if incoming request header indicates
     *                             invocation of proxy-providing method on a previous node
     * @param req - HTTP request
     * @param session - ongoing HTTP session instance
     */
    public void invokeHandlerByMethod(final boolean isForwardedRequest,
                                      @NotNull final Request req,
                                      @NotNull final HttpSession session) throws IOException {

        final String[] nodes;
        final String id = req.getParameter("id=");
        final ReplicationFactor repliFactorObj = ReplicationFactor
                .getRepliFactor(req.getParameter("replicas"), repliFactor, session);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (isForwardedRequest) {
            nodes = new String[]{topology.getThisNode()
            };
        } else {
            nodes = topology.replicasFor(key, repliFactorObj.getFromValue());
        }

        futUtils.setForwardedRequest(isForwardedRequest);
        try {
            switch (req.getMethod()) {
                case Request.METHOD_GET:
                    getWithMultipleNodes(nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                case Request.METHOD_PUT:
                    upsertWithMultipleNodes(nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                case Request.METHOD_DELETE:
                    deleteWithMultipleNodes(nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, RepliServiceImpl.REJECT_METHOD_ERROR_LOG);
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, GATEWAY_TIMEOUT_ERROR_LOG);
        }
    }
}
