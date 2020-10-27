package ru.mail.polis.service;

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

final class ReplicationServiceUtils {
    private ReplicationServiceUtils() { }

    private static Value syncValues(final List<Value> values) {

        if (values.size() == 1) {
            return values.get(0);
        }

        return values.stream()
                .filter(value -> !value.isValueMissing())
                .max(Comparator.comparingLong(Value::getTimestamp))
                .orElseGet(Value::resolveMissingValue);
    }

    static String[] getNodeReplica(
            @NotNull final ByteBuffer key,
            @NotNull final ReplicationFactor replicationFactor,
            final boolean isForwardedRequest,
            @NotNull final Topology topology) {

        return isForwardedRequest ? new String[]{
                topology.getCurrentNode()
        } : topology.getReplicas(key, replicationFactor.getFrom());
    }

    static Response handleExternal(
            final List<Value> values,
            final String[] nodeReplicas,
            final boolean isForwardedRequest) throws IOException {

        final Value value = syncValues(values);

        if (value.isValueDeleted()) {
            return new Response(Response.NOT_FOUND, value.getValueBytes());
        }

        if (nodeReplicas.length == 1 && isForwardedRequest) {
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
