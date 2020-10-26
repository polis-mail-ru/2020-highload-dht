package ru.mail.polis.service.codearound;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class ReplicationLsm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationLsm.class);
    private static final String NORMAL_REQUEST_HEADER = "/v0/entity?id=";
    private static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    private static final String IO_ERROR_LOG = "IO exception raised";
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    private static final String CASE_FORWARDING_ERROR_LOG = "Error forwarding request via proxy";
    private final DAO dao;
    private final Topology<String> topology;
    private final Map<String, HttpClient> nodesToClients;
    private final ReplicationFactor repliFactor;

    ReplicationLsm(@NotNull final DAO dao,
            @NotNull final ExecutorService exec,
            @NotNull final Topology<String> topology,
            Map<String, HttpClient> nodesToClients,
            @NotNull final ReplicationFactor repliFactor) {
        this.dao = dao;
        this.topology = topology;
        this.nodesToClients = nodesToClients;
        this.repliFactor = repliFactor;
    }

    /**
     * GET handler applicable for single node topology.
     *
     * @param key - key searched
     * @param req - HTTP request
     * @return HTTP response
     */
    Response getWithOnlyNode(@NotNull final ByteBuffer key, @NotNull final Request req) {
        final String owner = topology.primaryFor(key);
        ByteBuffer buf;
        if (topology.isThisNode(owner)) {
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
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * GET handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param repliFactor - replication factor
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response getWithMultipleNodes(final String id,
            @NotNull final ReplicationFactor repliFactor,
            final boolean isForwardedRequest) throws IOException {
        int replCounter = 0;
        final String[] nodes = RepliServiceUtils.getNodeReplica(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor,
                isForwardedRequest,
                topology);
        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            try {
                Response response;
                if (topology.isThisNode(node)) {
                    response = RepliServiceUtils.issueInternalResponse(
                            ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)),
                            dao);
                } else {
                    response = nodesToClients.get(node)
                            .get(NORMAL_REQUEST_HEADER + id, RepliServiceImpl.FORWARD_REQUEST_HEADER);
                }
                if (response.getStatus() == 404 && response.getBody().length == 0) {
                    values.add(Value.resolveMissingValue());
                } else if (response.getStatus() == 500) {
                    continue;
                } else {
                    values.add(Value.getValueFromBytes(response.getBody()));
                }
                replCounter++;
            } catch (HttpException | PoolException | InterruptedException exc) {
                LOGGER.error("Error running GET handler on cluster replica node", exc);
            }
        }
        if (isForwardedRequest || replCounter >= repliFactor.getAckValue()) {
            return RepliServiceUtils.issueExternalResponse(values, nodes, isForwardedRequest);
        } else {
            LOGGER.error(RepliServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * PUT handler impl for single node topology.
     *
     * @param key - target key
     * @param byteVal byte array processed as a key-bound value
     * @param req - HTTP request
     * @return HTTP response
     */
    Response upsertWithOnlyNode(
            @NotNull final ByteBuffer key,
            final byte[] byteVal,
            @NotNull final Request req) {
        final String owner = topology.primaryFor(key);
        final ByteBuffer val = ByteBuffer.wrap(byteVal);
        if (topology.isThisNode(owner)) {
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
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * PUT handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param value byte array processed as a key-bound value
     * @param ackValue - replication quorum factor ('ack' parameter)
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response upsertWithMultipleNodes(
            final String id,
            final byte[] value,
            final int ackValue,
            final boolean isForwardedRequest) throws IOException {
        if (isForwardedRequest) {
            try {
                dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException exc) {
                return new Response(Response.INTERNAL_ERROR, exc.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final String[] nodes = topology.replicasFor(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isThisNode(node)) {
                    dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                    ack++;
                } else {
                    final Response response = nodesToClients.get(node)
                            .put(NORMAL_REQUEST_HEADER + id, value, RepliServiceImpl.FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 201) {
                        ack++;
                    }
                }
            } catch (IOException | PoolException | InterruptedException | HttpException exc) {
                LOGGER.error("Error running PUT handler on cluster replica node", exc);
            }
        }
        if (ack >= ackValue) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            LOGGER.error(RepliServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * DELETE handler impl for single node topology.
     *
     * @param key - target key
     * @param req HTTP request
     * @return HTTP response
     */
    Response deleteWithOnlyNode(
            @NotNull final ByteBuffer key,
            @NotNull final Request req) throws IOException {
        final String owner = topology.primaryFor(key);
        if (topology.isThisNode(owner)) {
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
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * DELETE handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param ackValue - replication quorum factor ('ack' parameter)
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response deleteWithMultipleNodes(
            final String id,
            final int ackValue,
            final boolean isForwardedRequest) throws IOException {
        if (isForwardedRequest) {
            try {
                dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, e.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final String[] nodes = topology.replicasFor(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isThisNode(node)) {
                    dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                    ack++;
                } else {
                    final Response response = nodesToClients.
                            get(node)
                            .delete(NORMAL_REQUEST_HEADER + id, RepliServiceImpl.FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 202) {
                        ack++;
                    }
                }
                if (ack == ackValue) {
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                }
            } catch (IOException | PoolException | HttpException | InterruptedException exc) {
                LOGGER.error("Error running DELETE handler on cluster replica node", exc);
            }
        }
        LOGGER.error(RepliServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * implements request proxying in case of mismatching current receiver ID (self ID) and target one.
     *
     * @param nodeId request forwarding node ID
     * @param req HTTP request
     */
    private Response proxy(@NotNull final String nodeId, @NotNull final Request req) {
        try {
            req.addHeader("X-Proxy-For: " + nodeId);
            return nodesToClients.get(nodeId).invoke(req);
        } catch (IOException | InterruptedException | HttpException | PoolException exc) {
            LOGGER.error(CASE_FORWARDING_ERROR_LOG, exc);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
