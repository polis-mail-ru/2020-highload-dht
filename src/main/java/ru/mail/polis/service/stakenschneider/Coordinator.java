package ru.mail.polis.service.stakenschneider;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.RocksDAO;
import ru.mail.polis.dao.TimestampRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class Coordinator {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    private static final String PROXY_HEADER = "X-OK-Proxy: True";

    private final RocksDAO dao;
    private final Nodes nodes;
    private final Map<String, HttpClient> clusterClients;

    /**
     * Cluster coordinator instance.
     *
     * @param nodes          - to specify cluster nodes
     * @param clusterClients - to specify the HttpClients of the cluster
     * @param dao            - to specify current DAO
     */
    public Coordinator(@NotNull final Nodes nodes,
                       @NotNull final Map<String, HttpClient> clusterClients,
                       @NotNull final DAO dao) {
        this.dao = (RocksDAO) dao;
        this.nodes = nodes;
        this.clusterClients = clusterClients;
    }

    /**
     * Coordinate the delete among all clusters.
     *
     * @param replicaNodes to define the nodes where to create replicas
     * @param request      to define request
     * @param acks         to specify the amount of acks needed
     * @return Response value
     */
    public Response coordinateDelete(final Set<String> replicaNodes,
                                     @NotNull final Request request,
                                     final int acks) {
        final String id = request.getParameter("id=");
        final boolean proxied = request.getHeader(PROXY_HEADER) != null;
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;
        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    deleteWithTimestampMethodWrapper(key);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node).invoke(request);
                    if (resp.getStatus() == 202) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | InterruptedException | PoolException e) {
                log.warn("Exception while deleting by proxy: ", e);
            }
        }
        if (asks >= acks || proxied) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * Coordinate the put among all clusters.
     *
     * @param replicaNodes to define the nodes where to create replicas
     * @param request      to define request
     * @param acks         to specify the amount of acks needed
     * @return Response value
     */
    public Response coordinatePut(final Set<String> replicaNodes,
                                  @NotNull final Request request,
                                  final int acks) {
        final String id = request.getParameter("id=");
        final boolean proxied = request.getHeader(PROXY_HEADER) != null;
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;
        request.addHeader(PROXY_HEADER);
        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    putWithTimestampMethodWrapper(key, request);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node).invoke(request);
                    if (resp.getStatus() == 201) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | PoolException | InterruptedException e) {
                log.warn("Exception while putting", e);
            }
        }
        if (asks >= acks || proxied) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * Coordinate the get among all clusters.
     *
     * @return Response value
     */
    public Response coordinateGet(final Set<String> replicaNodes,
                                  @NotNull final Request request,
                                  final int acks) throws IOException {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;
        final List<TimestampRecord> responses = new ArrayList<>();
        final boolean proxied = request.getHeader(PROXY_HEADER) != null;
        request.addHeader(PROXY_HEADER);
        for (final String node : replicaNodes) {
            try {
                Response respGet;
                if (node.equals(nodes.getId())) {
                    respGet = getWithTimestampMethodWrapper(key);
                } else {
                    respGet = clusterClients.get(node).invoke(request);
                }
                if (respGet.getStatus() == 404 && respGet.getBody().length == 0) {
                    responses.add(TimestampRecord.getEmpty());
                } else if (respGet.getStatus() == 500) {
                    continue;
                } else {
                    responses.add(TimestampRecord.fromBytes(respGet.getBody()));
                }
                asks++;
            } catch (HttpException | PoolException | InterruptedException e) {
                log.warn("Exception while putting", e);
            }
        }
        if (asks >= acks || proxied) {
            return processResponses(replicaNodes, responses, proxied);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    private Response processResponses(final Set<String> replicaNodes,
                                      final List<TimestampRecord> responses,
                                      final boolean proxied) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);
        if (mergedResp.isValue()) {
            if (!proxied && replicaNodes.size() == 1) {
                return new Response(Response.OK, mergedResp.getValueAsBytes());
            } else if (proxied && replicaNodes.size() == 1) {
                return new Response(Response.OK, mergedResp.toBytes());
            } else {
                return new Response(Response.OK, mergedResp.getValueAsBytes());
            }
        } else if (mergedResp.isDeleted()) {
            return new Response(Response.NOT_FOUND, mergedResp.toBytes());
        } else {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private void putWithTimestampMethodWrapper(final ByteBuffer key,
                                               final Request request) throws IOException {
        dao.upsertRecordWithTimestamp(key, ByteBuffer.wrap(request.getBody()));
    }

    private void deleteWithTimestampMethodWrapper(final ByteBuffer key) throws IOException {
        dao.removeRecordWithTimestamp(key);
    }

    @NotNull
    private Response getWithTimestampMethodWrapper(final ByteBuffer key) {
        try {
            final byte[] res = copyAndExtractWithTimestampFromByteBuffer(key);
            return new Response(Response.OK, res);
        } catch (NoSuchElementException | IOException exp) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private byte[] copyAndExtractWithTimestampFromByteBuffer(@NotNull final ByteBuffer key)
            throws IOException {
        final TimestampRecord res = dao.getRecordWithTimestamp(key);
        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }
        return res.toBytes();
    }

    /**
     * Coordinate the request among all clusters.
     *
     */
    public void coordinateRequest(final Set<String> replicaClusters,
                                  final Request request,
                                  final int acks,
                                  final HttpSession session) throws IOException {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    session.sendResponse(coordinateGet(replicaClusters, request, acks));
                    break;
                case Request.METHOD_PUT:
                    session.sendResponse(coordinatePut(replicaClusters, request, acks));
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(coordinateDelete(replicaClusters, request, acks));
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, e.getMessage());
        }
    }
}
