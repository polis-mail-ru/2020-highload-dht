package ru.mail.polis.service;

import com.google.common.collect.ImmutableSet;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

final class ReplicationServiceUtils {
    private static final String TIMESTAMP = "Timestamp: ";

    private ReplicationServiceUtils() {

    }

    static Value syncValues(final List<Value> values) {
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

    static Response handleExternal(
            final List<Value> values
    ) throws IOException {
        return Value.toResponse(values);
    }

    static long getTimestamp(final Response response) throws NumberFormatException {
        final String timestamp = response.getHeader(TIMESTAMP);
        return timestamp == null ? -1 : Long.parseLong(timestamp);
    }

    static Response addTimestampHeader(final Response response, final long timestamp) {
        response.addHeader(TIMESTAMP + timestamp);
        return response;
    }

    static Response handleInternal(
            @NotNull final ByteBuffer key,
            @NotNull final DAO dao) {

        try {
            final Value value = dao.getValue(key);

            if (value.isValueMissing()) {
                new Response(Response.NOT_FOUND, Response.EMPTY);
            }

            if (value.isValueDeleted()) {
                final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                return addTimestampHeader(response, value.getTimestamp());
            }

            final Response response = new Response(Response.OK, value.getBytes());
            return addTimestampHeader(response, value.getTimestamp());
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }
}
