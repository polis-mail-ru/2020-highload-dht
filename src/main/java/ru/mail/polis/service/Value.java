package ru.mail.polis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Value {

    private static final Logger LOGGER = LoggerFactory.getLogger(Value.class);

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
        return new Value(true, timestamp, ByteBuffer.allocate(0));
    }

    static boolean isDeleted(final ByteBuffer buffer, final String marker) {
        final byte[] value = Util.toByteArray(buffer);
        final byte[] markerValue = marker.getBytes(StandardCharsets.UTF_8);
        return Arrays.equals(value, markerValue);
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
            LOGGER.info("Record was removed");
            throw new IOException();
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
}
