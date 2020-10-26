package ru.mail.polis.service.codearound;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public final class RepliServiceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepliServiceImpl.class);

    /**
     * class instance const.
     */
    private RepliServiceUtils() {

    }

    /**
     * performs synchronizing record replicas through nodes.
     *
     * @param values - collection of values ever pushed to storage
     * @return Value instance
     */
    public static Value syncReplicaValues(final List<Value> values) {

        if (values.size() == 1) {
            return values.get(0);
        } else {
            return values.stream()
                    .filter(value -> !value.isValueMissing())
                    .max(Comparator.comparingLong(Value::getTimestamp))
                    .orElseGet(Value::resolveMissingValue);
        }
    }

    /**
     * retrieves reference on array filled with same-key replicas.
     *
     * @param key - key searched through nodes within the cluster
     * @param repliFactor - replication factor
     * @param isForwardedRequest - true if incoming request header indicates
     *                             invocation of proxy-providing method on a previous node
     * @param topology - implementable cluster topology
     * @return array of replicas
     */
    public static String[] getNodeReplica(
            @NotNull final ByteBuffer key,
            @NotNull final ReplicationFactor repliFactor,
            final boolean isForwardedRequest,
            @NotNull final Topology<String> topology) {

        String[] nodeReplicas;

        if (isForwardedRequest) {
            nodeReplicas = new String[]{ topology.getThisNode() };
        } else {
            nodeReplicas = topology.replicasFor(key, repliFactor.getFromValue());
        }

        return nodeReplicas;
    }

    /**
     * issues some response for GET request upon processing replica (if any) pushed to external instance of storage.
     *
     * @param values - collection of values ever pushed to storage
     * @param nodeReplicas - array of replicas
     * @param isForwardedRequest - true if incoming request header indicates
     *                                 invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    public static Response issueExternalResponse(
            final List<Value> values,
            final String[] nodeReplicas,
            final boolean isForwardedRequest) throws IOException {

        final Value value = syncReplicaValues(values);

        if (value.isValueDeleted()) {
            return new Response(Response.NOT_FOUND, value.getBytesFromValue());
        } else {
            if (nodeReplicas.length == 1 && isForwardedRequest) {
                return new Response(Response.OK, value.getBytesFromValue());
            } else {
                return new Response(Response.OK, value.getBytes());
            }
        }
    }

    /**
     * issues some response for GET request upon processing replica (if any) pushed to internal instance of storage.
     *
     * @param key - key searched
     * @param dao - implementable DAO
     * @return HTTP response
     */
    public static Response issueInternalResponse(
            @NotNull final ByteBuffer key,
            @NotNull final DAO dao) {

        try {
            final Value value = dao.getValue(key);
            return new Response(Response.OK, value.getBytesFromValue());
        } catch (IOException exc) {
            LOGGER.error("IO exception raised when attempted reading on local node replica");
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException exc) {
            LOGGER.error("No match key found on local node replica");
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }
}
