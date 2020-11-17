package ru.mail.polis.service;

import com.google.common.collect.ImmutableSet;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class ReplicationServiceUtils {
    private static final String TIMESTAMP = "Timestamp: ";

    private ReplicationServiceUtils() {

    }

    /**
     * This synchronizes received values.
     * @param values - values list
     * @return - synchronized value
     */
    static Value syncValues(final List<Value> values) {
        return values.stream()
                .filter(value -> !value.isValueMissing())
                .max(Comparator.comparingLong(Value::getTimestamp))
                .orElseGet(Value::resolveMissingValue);
    }

    static Response addTimestampHeader(final Response response, final long timestamp) {
        response.addHeader(TIMESTAMP + timestamp);
        return response;
    }

}
