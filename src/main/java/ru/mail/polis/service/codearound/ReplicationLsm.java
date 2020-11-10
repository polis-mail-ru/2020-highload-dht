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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 *  class to feature topology-bound implementations of project-required DAO methods (get, put, delete).
 */
public class ReplicationLsm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationLsm.class);
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    public static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final HttpClient client;
    @NotNull
    private final ExecutorService exec;
    private AsyncConnectUtils response;

    /**
     * class const.
     *
     * @param dao - DAO instance
     * @param topology - topology implementation instance
     * @param exec - service to run threads
     * @param client - async Java http client
     */
    ReplicationLsm(@NotNull final DAO dao,
                   @NotNull final Topology<String> topology,
                   @NotNull final ExecutorService exec,
                   @NotNull final HttpClient client) {
        this.dao = dao;
        this.topology = topology;
        this.exec = exec;
        this.client = client;
    }

    /**
     * GET handler applicable for single node topology.
     *
     * @param key - key searched
     * @return HTTP response
     */
    Response getWithOnlyNode(@NotNull final ByteBuffer key) {
        final ByteBuffer buf;
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
            LOGGER.error(RepliServiceImpl.IO_ERROR_LOG);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * GET handler to run async processing in multi-node cluster.
     *
     * @param key - key searched
     * @param nodes - array of node IDs the cluster is build upon
     * @param req - HTTP request
     * @param ack - number of nodes (quorum) to issue success response when processing over replicas
     * @param session - ongoing HTTP session instance
     *
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void getWithMultipleNodes(@NotNull final ByteBuffer key,
                              final String[] nodes,
                              @NotNull final Request req,
                              final int ack,
                              @NotNull final HttpSession session) throws IOException {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            if (node.equals(topology.getThisNode())) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            final Response responses;
                            try {
                                responses = dao.getValueWithFutures(key);
                                return new AsyncConnectUtils().setReturnCode(responses.getStatus())
                                        .setBody(responses.getBody());
                            } catch (IOException exc) {
                                response = new AsyncConnectUtils().setReturnCode(404);
                                return response;
                            }
                        }, exec));
            } else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req).GET().build();
                final CompletableFuture<HttpResponse<byte[]>> responses =
                        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }

        try {
              session.sendResponse(FutureUtils.execGetWithFutures(values, futures, nodes, ack, req, exec));
        } catch (IOException exc) {
              LOGGER.error(FutureUtils.GET_COMPLETION_ERROR_LOG);
        }
    }

    /**
     * PUT handler impl for single node topology.
     *
     * @param key - target key
     * @param byteVal - byte array processed as a key-bound value
     * @return HTTP response
     */
    Response upsertWithOnlyNode(@NotNull final ByteBuffer key, final byte[] byteVal) {
        final ByteBuffer val = ByteBuffer.wrap(byteVal);
        try {
            dao.upsert(key, val);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
            return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error(RepliServiceImpl.IO_ERROR_LOG);
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
    @SuppressWarnings("FutureReturnValueIgnored")
    void upsertWithMultipleNodes(@NotNull final ByteBuffer key,
                                 final String[] nodes,
                                 @NotNull final Request req,
                                 final int count,
                                 @NotNull final HttpSession session) throws IOException {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        for (final String node : nodes) {
            if (topology.isThisNode(node)) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.upsertValueWithFutures(key, req);
                                return new AsyncConnectUtils().setReturnCode(201);
                            } catch (IOException exc) {
                                response = new AsyncConnectUtils().setReturnCode(404);
                                return response;
                            }
                        }, exec));
            } else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(req.getBody()))
                        .build();
                final CompletableFuture<HttpResponse<byte[]>> responses =
                        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }

       try {
           session.sendResponse(FutureUtils.execUpsertWithFutures(count, futures));
       } catch (IOException exc) {
             LOGGER.error(FutureUtils.UPSERT_COMPLETION_ERROR_LOG);
       }
    }

    /**
     * DELETE handler impl for single node topology.
     *
     * @param key - target key
     * @return HTTP response
     */
    Response deleteWithOnlyNode(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (RejectedExecutionException exc) {
            LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
            return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error(RepliServiceImpl.IO_ERROR_LOG);
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
    @SuppressWarnings("FutureReturnValueIgnored")
    void deleteWithMultipleNodes(@NotNull final ByteBuffer key,
                                 final String[] nodes,
                                 @NotNull final Request req,
                                 final int count,
                                 final HttpSession session) throws IOException {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(nodes.length);
        for (final String node : nodes) {
            if (topology.isThisNode(node)) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                dao.deleteValueWithFutures(key);
                                return new AsyncConnectUtils().setReturnCode(202);
                            } catch (IOException exc) {
                                response = new AsyncConnectUtils().setReturnCode(404);
                                return response;
                            }
                        }, exec));
            } else {
                final HttpRequest request = FutureUtils.setRequestPattern(node, req).DELETE().build();
                final CompletableFuture<HttpResponse<byte[]>> responses =
                        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
                futures.add(responses);
            }
        }

       try {
            session.sendResponse(FutureUtils.execDeleteWithFutures(count, futures));
        } catch (IOException exc) {
            LOGGER.error(FutureUtils.DELETE_COMPLETION_ERROR_LOG);
       }
    }
}
