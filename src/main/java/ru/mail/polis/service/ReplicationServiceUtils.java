package ru.mail.polis.service;

import com.google.common.collect.ImmutableSet;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

final class ReplicationServiceUtils {
    private ReplicationServiceUtils() {

    }

    private static Value syncValues(final List<Value> values) {

        if (values.size() == 1) {
            return values.get(0);
        }

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

        return isForwardedRequest ? new HashSet<>(ImmutableSet.of(
                topology.getCurrentNode()
        )) : topology.getReplicas(key, replicationFactor.getFrom());
    }

    static Response handleExternal(
            final List<Value> values,
            final Set<String> nodeReplicas,
            final boolean isForwardedRequest) throws IOException {

        final Value value = syncValues(values);

        if (value.isValueDeleted()) {
            return new Response(Response.NOT_FOUND, value.getValueBytes());
        }

        if (nodeReplicas.size() == 1 && isForwardedRequest) {
            return new Response(Response.OK, value.getValueBytes());
        }

        return new Response(Response.OK, value.getBytes());
    }

    static Response handleInternal(
            @NotNull final ByteBuffer key,
            @NotNull final DAO dao) {

        try {
            final Value value = dao.getValue(key);
            return new Response(Response.OK, value.getValueBytes());
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }
}
