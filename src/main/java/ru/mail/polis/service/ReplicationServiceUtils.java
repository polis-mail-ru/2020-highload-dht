package ru.mail.polis.service;

import com.google.common.collect.ImmutableSet;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ReplicationServiceUtils {
    private static final String TIMESTAMP = "Timestamp: ";

    private ReplicationServiceUtils() {

    }

    /**
     * This synchronizes received values.
     * @param values - values list
     * @return - synchronized value
     */
    public static Value syncValues(final List<Value> values) {
        return values.stream()
                .filter(value -> !value.isValueMissing())
                .max(Comparator.comparingLong(Value::getTimestamp))
                .orElseGet(Value::resolveMissingValue);
    }

    static Set<String> getNodeReplica(
            @NotNull final ByteBuffer key,
            @NotNull final ReplicationFactor replicationFactor,
            final boolean isForwardedRequest,
            @NotNull final Topology topology) throws NotEnoughNodesException {

        return isForwardedRequest ? ImmutableSet.of(
                topology.getCurrentNode()
        ) : topology.getReplicas(key, replicationFactor.getFrom());
    }

    static long getTimestamp(final Response response) throws NumberFormatException {
        final String timestamp = response.getHeader(TIMESTAMP);
        return timestamp == null ? -1 : Long.parseLong(timestamp);
    }

    static Response addTimestampHeader(final Response response, final long timestamp) {
        response.addHeader(TIMESTAMP + timestamp);
        return response;
    }

}
