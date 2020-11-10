package ru.mail.polis.service;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
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
import java.util.Set;

import static java.util.Map.entry;
import static ru.mail.polis.service.ReplicationServiceUtils.getNodeReplica;
import static ru.mail.polis.service.ReplicationServiceUtils.handleExternal;
import static ru.mail.polis.service.ReplicationServiceUtils.handleInternal;

class ReplicationHandler {

    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);
    private static final String NORMAL_REQUEST_HEADER = "/v0/entity?id=";
    private final DAO dao;
    private final Topology topology;
    private final Map<String, HttpClient> nodesToClients;
    private final ReplicationFactor replicationFactor;

    private enum ErrorNames {
        NOT_FOUND_ERROR, IO_ERROR, QUEUE_LIMIT_ERROR, PROXY_ERROR
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.NOT_FOUND_ERROR, "Value not found"),
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.QUEUE_LIMIT_ERROR, "Queue is full"),
            entry(ErrorNames.PROXY_ERROR, "Error forwarding request via proxy")
    );

    ReplicationHandler(
            @NotNull final DAO dao,
            @NotNull final Topology topology,
            @NotNull final Map<String, HttpClient> nodesToClients,
            @NotNull final ReplicationFactor replicationFactor
    ) {
        this.dao = dao;
        this.topology = topology;
        this.nodesToClients = nodesToClients;
        this.replicationFactor = replicationFactor;
    }

    Response multipleGet(
            final String id, @NotNull final ReplicationFactor repliFactor, final boolean isForwardedRequest
    ) throws NotEnoughNodesException, IOException {
        final Set<String> nodes = getNodeReplica(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor,
                isForwardedRequest,
                topology
        );

        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            try {
                Response response;
                if (topology.isSelfId(node)) {
                    response = handleInternal(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)), dao);
                } else {
                    response = nodesToClients.get(node)
                            .get(NORMAL_REQUEST_HEADER + id, ReplicationServiceImpl.FORWARD_REQUEST_HEADER);
                }
                values.add(Value.fromResponse(response));
            } catch (HttpException | PoolException | InterruptedException | IOException | NumberFormatException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), exc);
            }
        }

        return processGet(isForwardedRequest, repliFactor, values.size(), nodes, values);
    }

    private Response processGet(
            final boolean isForwardedRequest, final ReplicationFactor repliFactor, final int replCounter,
            final Set<String> nodes, final List<Value> values
    ) throws IOException {
        if (isForwardedRequest || replCounter >= repliFactor.getAck()) {
            return handleExternal(values, nodes, isForwardedRequest);
        } else {
            log.error(ReplicationServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    Response multipleUpsert(
            final String id, final byte[] value, final int ackValue, final boolean isForwardedRequest
    ) throws NotEnoughNodesException {
        if (isForwardedRequest) {
            try {
                dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException exc) {
                return new Response(Response.INTERNAL_ERROR, exc.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final Set<String> nodes = topology.getReplicas(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                replicationFactor.getFrom());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isSelfId(node)) {
                    dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                    ack++;
                } else {
                    final Response response = nodesToClients.get(node)
                            .put(NORMAL_REQUEST_HEADER + id, value, ReplicationServiceImpl.FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 201) {
                        ack++;
                    }
                }
            } catch (IOException | PoolException | InterruptedException | HttpException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), exc);
            }
        }
        if (ack >= ackValue) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            log.error(ReplicationServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    Response multipleDelete(
            final String id,
            final int ackValue,
            final boolean isForwardedRequest) throws NotEnoughNodesException {
        if (isForwardedRequest) {
            try {
                dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, e.toString().getBytes(Charset.defaultCharset()));
            }
        }

        final Set<String> nodes = topology.getReplicas(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                replicationFactor.getFrom());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isSelfId(node)) {
                    dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                    ack++;
                } else {
                    final Response response = nodesToClients
                            .get(node)
                            .delete(NORMAL_REQUEST_HEADER + id, ReplicationServiceImpl.FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 202) {
                        ack++;
                    }
                }
                if (ack == ackValue) {
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                }
            } catch (IOException | PoolException | HttpException | InterruptedException exc) {
                log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), exc);
            }
        }
        log.error(ReplicationServiceImpl.GATEWAY_TIMEOUT_ERROR_LOG);
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

}
