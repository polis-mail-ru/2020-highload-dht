package ru.mail.polis.service;

import one.nio.http.Response;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public final class Value {
    private static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private final boolean isValueDeleted;
    private final long timestamp;
    private final ByteBuffer buffer;

    private Value(
            final boolean isValueDeleted,
            final long timestamp,
            final ByteBuffer buffer
    ) {

        this.isValueDeleted = isValueDeleted;
        this.timestamp = timestamp;
        this.buffer = buffer;
    }

    public static Value resolveExistingValue(final ByteBuffer buffer, final long timestamp) {
        return new Value(false, timestamp, buffer);
    }

    public static Value resolveDeletedValue(final long timestamp) {
        return new Value(true, timestamp, EMPTY_BUFFER);
    }

    static Value resolveMissingValue() {
        return new Value(false, -1, null);
    }

    boolean isValueDeleted() {
        return isValueDeleted;
    }

    boolean isValueMissing() {
        return buffer == null;
    }

    long getTimestamp() {
        return timestamp;
    }

    private ByteBuffer getValue() throws IOException {
        if (isValueDeleted) {
            throw new IOException("Record was removed");
        } else {
            return buffer;
        }
    }

    byte[] getBytes() throws IOException {
        final ByteBuffer buf = getValue().duplicate();
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    /**
     * Creates new Value from bytes.
     * @param bytes - from what to create Value
     * @return Value
     */
    public static Value composeFromBytes(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final short isValueDeleted = buffer.getShort();
        final long timestamp = buffer.getLong();

        return new Value(isValueDeleted == 1, timestamp, buffer);
    }

    /**
     * Retrieves value written to byte[] with specific timestamp attribute.
     *
     * @return timestamp-exposing value written to byte array
     */
    public byte[] getValueBytes() {
        final short isDeleted = isValueDeleted ? (short) 1 : (short) -1;

        return ByteBuffer.allocate(Short.BYTES + Long.BYTES + buffer.remaining())
                .putShort(isDeleted)
                .putLong(timestamp)
                .put(buffer.duplicate()).array();
    }

    static Value fromResponse(final Response response) {
        final long timestamp = ReplicationServiceUtils.getTimestamp(response);

        if (response.getStatus() == 200) {
            return Value.resolveExistingValue(ByteBuffer.wrap(response.getBody()), timestamp);
        }

        if (response.getStatus() == 404 && timestamp > 0) {
            return Value.resolveDeletedValue(timestamp);
        }

        return Value.resolveMissingValue();
    }

    static Response toResponse(
            final Set<String> nodes,
            final List<Value> responses,
            final boolean isForwardedRequest
    ) throws IOException {
        final Value value = ReplicationServiceUtils.syncValues(responses);
        // Value is deleted
        if (value.isValueDeleted()) {
            final Response response = new Response(Response.NOT_FOUND, value.getValueBytes());
            return ReplicationServiceUtils.addTimestampHeader(response, value.getTimestamp());
        }
        // Value is present
        Response response;
        if (!value.isValueMissing()) {
            if (isForwardedRequest && nodes.size() == 1) {
                response = new Response(Response.OK, value.getValueBytes());
                return ReplicationServiceUtils.addTimestampHeader(response, value.getTimestamp());
            }

            response = new Response(Response.OK, value.getBytes());
            return ReplicationServiceUtils.addTimestampHeader(response, value.getTimestamp());
        }
        // Value is missing
        return new Response(Response.NOT_FOUND, Response.EMPTY);
    }
}
