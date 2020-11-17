package ru.mail.polis.service.basta123;

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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * class to feature topology-bound implementations of project-required DAO methods (get, put, delete).
 */
public class HelperReplicHttpServerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelperReplicHttpServerImpl.class);
    private static final String REQUEST_HEADER = "/v0/entity?id=";
    private static final String CANT_SEND_RESPONSE = "CANT_SEND_RESPONSE";
    private final DAO dao;
    private final Topology<String> topology;
    private final Map<String, HttpClient> clientAndNode;

    /**
     * class const.
     *
     * @param dao           - DAO instance
     * @param topology      - topology implementation instance
     * @param clientAndNode - HashMap-implemented mapping available nodes over HTTP clients
     */
    HelperReplicHttpServerImpl(@NotNull final DAO dao,
                               @NotNull final Topology<String> topology,
                               final Map<String, HttpClient> clientAndNode) {
        this.dao = dao;
        this.topology = topology;
        this.clientAndNode = clientAndNode;
    }

    Response getTimestampValue(final String id) throws IOException {
        try {
            final TimestampValue timestampValue = dao.getTimestampValue(ByteBuffer.wrap
                    (id.getBytes(StandardCharsets.UTF_8)));
            return new Response(Response.OK,
                    TimestampValue.getBytesFromTimestampValue(timestampValue.isValueDeleted(),
                            timestampValue.getTimeStamp(), timestampValue.getBuffer()));
        } catch (NoSuchElementException exc) {
            LOGGER.error("no key found");
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    Response getFromReplicas(final String id,
                             @NotNull final AckFrom ackFrom,
                             final boolean requestForward) throws IOException {
        if (requestForward) {
            try {
                return getTimestampValue(id);
            } catch (NoSuchElementException exc) {
                LOGGER.error("no key found");
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }
        final List<String> nodes = topology.getNodesForKey(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                ackFrom.getFromValue());
        final List<TimestampValue> TimestampValues = new ArrayList<>();
        int ack = 0;
        for (final String node : nodes) {
            try {
                Response response;
                if (topology.isLocal(node)) {
                        response = getTimestampValue(id);
                } else {
                    response = clientAndNode.get(node)
                            .get(REQUEST_HEADER + id, ReplicHttpServerImpl.FORWARD_REQ);
                }
                if (response.getStatus() == 404 && response.getBody().length == 0) {
                    TimestampValues.add(TimestampValue.getTimestampValue());
                } else if (response.getStatus() == 500) {
                    continue;
                } else {
                    TimestampValues.add(TimestampValue.getTimestampValueFromBytes(response.getBody()));
                }
                ack++;
            } catch (HttpException | PoolException | InterruptedException exc) {
                LOGGER.error("error get: ", exc);
            }
        }
        return checkAcks(ackFrom, ack, TimestampValues);
    }

    private Response checkAcks(@NotNull final AckFrom ackFrom, int ack, List<TimestampValue> TimestampValues)
    {
        if (ack >= ackFrom.getAckValue()) {
            final TimestampValue timestampValue = valuesSync(TimestampValues);
            if (timestampValue.isValueDeleted()) {
                return new Response(Response.NOT_FOUND,
                        TimestampValue.getBytesFromTimestampValue(timestampValue.isValueDeleted(),
                                timestampValue.getTimeStamp(), timestampValue.getBuffer()));
            } else {
                final ByteBuffer byteBuffer = timestampValue.getBuffer();
                final byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                return new Response(Response.OK, bytes);
            }
        } else {
            LOGGER.error(ReplicHttpServerImpl.TIMEOUT_ERROR);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    Response get(@NotNull final String id,
                 final Request request) throws IOException {
        final ByteBuffer keyByteBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        ByteBuffer valueByteBuffer;
        final byte[] valueBytes;

        final String endNode = topology.getNodeForKey(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                valueByteBuffer = dao.get(keyByteBuffer);
                valueBytes = Utils.readArrayBytes(valueByteBuffer);
                return new Response(Response.ok(valueBytes));
            } catch (IOException e) {
                LOGGER.error("get error: ", e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            } catch (NoSuchElementException e) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        } else {
            return proxying(endNode, request);
        }
    }

    Response upsertToReplicas(
            final String id,
            final byte[] value,
            final AckFrom ackFrom,
            final boolean requestForward) throws IOException {
        if (requestForward) {
            try {
                dao.upsertTimestampValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                        ByteBuffer.wrap(value));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException exc) {
                return new Response(Response.INTERNAL_ERROR, exc.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final List<String> nodes = topology.getNodesForKey(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                ackFrom.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isLocal(node)) {
                    dao.upsertTimestampValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                            ByteBuffer.wrap(value));
                    ack++;
                } else {
                    final Response response = clientAndNode.get(node)
                            .put(REQUEST_HEADER + id, value, ReplicHttpServerImpl.FORWARD_REQ);
                    if (response.getStatus() == 201) {
                        ack++;
                    }
                }
            } catch (IOException | PoolException | InterruptedException | HttpException exc) {
                LOGGER.error("Error put: ", exc);
            }
        }
        if (ack >= ackFrom.getAckValue()) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            LOGGER.error(ReplicHttpServerImpl.TIMEOUT_ERROR);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    Response put(@NotNull final String id,
                 @NotNull final Request request) throws IOException {

        final ByteBuffer keyByteBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final byte[] valueByte = request.getBody();
        final ByteBuffer valueByteBuffer = ByteBuffer.wrap(valueByte);
        final String endNode = topology.getNodeForKey(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                dao.upsert(keyByteBuffer, valueByteBuffer);
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } else {
            return proxying(endNode, request);
        }

    }

    Response deleteFromReplicas(
            final String id,
            final AckFrom ackFrom,
            final boolean requestForward) throws IOException {
        if (requestForward) {
            try {
                dao.removeTimestampValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, e.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final List<String> nodes = topology.getNodesForKey(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                ackFrom.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isLocal(node)) {
                    dao.removeTimestampValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                    ack++;
                } else {
                    final Response response = clientAndNode
                            .get(node)
                            .delete(REQUEST_HEADER + id, ReplicHttpServerImpl.FORWARD_REQ);
                    if (response.getStatus() == 202) {
                        ack++;
                    }
                }
                if (ack == ackFrom.getAckValue()) {
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                }
            } catch (IOException | PoolException | HttpException | InterruptedException exc) {
                LOGGER.error("Error delete:", exc);
            }
        }
        LOGGER.error(ReplicHttpServerImpl.TIMEOUT_ERROR);
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    Response delete(@NotNull final String id,
                    final Request request) throws IOException {
        final ByteBuffer keyByteBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final String endNode = topology.getNodeForKey(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                dao.remove(keyByteBuffer);
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException ex) {
                LOGGER.error("can't remove value: ", ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } else {
            return proxying(endNode, request);
        }
    }

    private Response proxying(@NotNull final String node,
                              final Request request) throws IOException {
        try {
            return clientAndNode.get(node).invoke(request);
        } catch (IOException | HttpException | InterruptedException | PoolException e) {
            LOGGER.error("error when proxying the request:", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Send response.
     *
     * @param httpSession - httpSession.
     * @param resultCode  - send resultCode to the user.
     */

    public static void sendResponse(@NotNull final HttpSession httpSession, final String resultCode) {
        try {
            httpSession.sendResponse(new Response(resultCode, Response.EMPTY));
        } catch (IOException e) {
            LOGGER.error(CANT_SEND_RESPONSE, e);
        }
    }

    /**
     * selects one from all nodes depending on the timestamp.
     *
     * @param valuesFromNodes - List of values.
     * @return valuesFromNodes instance.
     */
    public static TimestampValue valuesSync(final List<TimestampValue> valuesFromNodes) {
        if (valuesFromNodes.size() == 1) {
            return valuesFromNodes.get(0);
        } else {
            return valuesFromNodes.stream()
                    .filter(timestampValue -> !timestampValue.valueExists())
                    .max(Comparator.comparingLong(TimestampValue::getTimeStamp))
                    .orElseGet(TimestampValue::getTimestampValue);
        }
    }
}
