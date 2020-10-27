package ru.mail.polis.service.manikhin;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.pool.PoolException;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.TimestampRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.NoSuchElementException;

public class ReplicasRequests {
    private final DAO dao;
    private final Topology nodes;
    private final Map<String, HttpClient> clusterClients;
    private final Logger log = LoggerFactory.getLogger(ReplicasRequests.class);
    private static final String PROXY_HEADER = "X-OK-Proxy: True";
    private static final String ENTITY_HEADER = "/v0/entity?id=";

    ReplicasRequests(final DAO dao, final Map<String, HttpClient> clusterClients, final Topology nodes) {
        this.dao = dao;
        this.clusterClients = clusterClients;
        this.nodes = nodes;
    }

    public Response getTimestamp(@NotNull final ByteBuffer key) {
        try {
            final byte[] res = timestampFromByteBuffer(key);
            return new Response(Response.OK, res);
        } catch (NoSuchElementException | IOException exp) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    public void putTimestamp(@NotNull final ByteBuffer key,
                              @NotNull final Request request) throws IOException {
        dao.upsertTimestampRecord(key, ByteBuffer.wrap(request.getBody()));
    }

    public void deleteTimestamp(@NotNull final ByteBuffer key) throws IOException {
        dao.removeTimestampRecord(key);
    }

    public byte[] timestampFromByteBuffer(@NotNull final ByteBuffer key)
            throws IOException {
        final TimestampRecord res = dao.getTimestampRecord(key);
        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }
        return res.toBytes();
    }

    /**
     * Request handler for input requests with many replicas.
     *
     * @param replicaClusters - replica clusters
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     * @param session - http-session
     */
    public void handleMultiRequest(@NotNull final Set<String> replicaClusters,
                                   @NotNull final Request request,
                                   final int replicateAcks,
                                   @NotNull final HttpSession session) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    multiGet(session, replicaClusters, request, replicateAcks);
                    break;
                case Request.METHOD_PUT:
                    multiPut(session, replicaClusters, request, replicateAcks);
                    break;
                case Request.METHOD_DELETE:
                    multiDelete(session, replicaClusters, request, replicateAcks);
                    break;
                default:
                    sendResponse(session, new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    break;
            }
        } catch (IOException error) {
            log.error("handleMultiRequest error: ", error);
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    /**
     * Request handler for input GET-request with many replicas.
     *
     * @param session - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiGet(@NotNull final HttpSession session,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final Request request,
                         final int replicateAcks) throws IOException {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        final List<TimestampRecord> responses = new ArrayList<>();

        for (final String node : replicaNodes) {
            try {
                Response respGet;

                if (node.equals(nodes.getId())) {
                    respGet = getTimestamp(key);
                } else {
                    respGet = clusterClients.get(node).get(ENTITY_HEADER + id, PROXY_HEADER);
                }

                if (respGet.getStatus() == 404 && respGet.getBody().length == 0) {
                    responses.add(TimestampRecord.getEmpty());
                } else if (respGet.getStatus() == 500) {
                    continue;
                } else {
                    responses.add(TimestampRecord.fromBytes(respGet.getBody()));
                }

                asks++;
            } catch (HttpException | PoolException | InterruptedException error) {
                log.error("multiGet error", error);
            }
        }

        final boolean isForwardedRequest = request.getHeader(PROXY_HEADER) != null;

        if (asks >= replicateAcks || isForwardedRequest) {
            processResponses(session, replicaNodes, responses, isForwardedRequest);
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    /**
     * Request handler for input PUT-request with many replicas.
     *
     * @param session - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiPut(@NotNull final HttpSession session,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final Request request,
                         final int replicateAcks) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    putTimestamp(key, request);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node)
                            .put(ENTITY_HEADER + id, request.getBody(), PROXY_HEADER);
                    if (resp.getStatus() == 201) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | PoolException | InterruptedException error) {
                log.error("multiPut error", error);
            }
        }
        if (asks >= replicateAcks || request.getHeader(PROXY_HEADER) != null) {
            sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    /**
     * Request handler for input DELETE-request with many replicas.
     *
     * @param session - http-session
     * @param replicaNodes - replica nodes
     * @param request - input http-request
     * @param replicateAcks - input replicate acks
     */
    public void multiDelete(@NotNull final HttpSession session,
                            @NotNull final Set<String> replicaNodes,
                            @NotNull final Request request,
                            final int replicateAcks) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    deleteTimestamp(key);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node)
                            .delete(ENTITY_HEADER + id, PROXY_HEADER);
                    if (resp.getStatus() == 202) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | InterruptedException | PoolException error) {
                log.warn("multiDelete error: ", error);
            }
        }
        if (asks >= replicateAcks || request.getHeader(PROXY_HEADER) != null) {
            sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    public void processResponses(@NotNull final HttpSession session,
                                  @NotNull final Set<String> replicaNodes,
                                  @NotNull final List<TimestampRecord> responses,
                                  final boolean isForwardedRequest) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);

        if (mergedResp.isValue()) {
            if (!isForwardedRequest && replicaNodes.size() == 1) {
                sendResponse(session, new Response(Response.OK, mergedResp.getValueAsBytes()));
            } else if (isForwardedRequest && replicaNodes.size() == 1) {
                sendResponse(session, new Response(Response.OK, mergedResp.toBytes()));
            } else {
                sendResponse(session, new Response(Response.OK, mergedResp.getValueAsBytes()));
            }
        } else if (mergedResp.isDeleted()) {
            sendResponse(session, new Response(Response.NOT_FOUND, mergedResp.toBytes()));
        } else {
            sendResponse(session, new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    /**
     * Response sender for input requests.
     *
     * @param session - http-session
     * @param response - response object on input request
     */
    public void sendResponse(@NotNull final HttpSession session,
                              @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException error) {
            log.error("Sending response error: ", error);
        }
    }
}
