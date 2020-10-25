package ru.mail.polis.service.codearound;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class Value {

    private final boolean isValueDeleted;
    private final long timestamp;
    private final ByteBuffer value;

    private Value(final boolean isValueDeleted,
                  final long timestamp,
                  final ByteBuffer value) {

        this.isValueDeleted = isValueDeleted;
        this.timestamp = timestamp;
        this.value = value;
    }

    public static Value resolveExistingValue(final ByteBuffer value, final long timestamp) {
        return new Value(false, timestamp, value);
    }

    public static Value resolveDeletedValue(final long timestamp) {
        return new Value(true, timestamp, ByteBuffer.allocate(0));
    }

    static Value resolveMissingValue() {
        return new Value(false, -1, null);
    }

    boolean isValueDeleted() {
        return isValueDeleted;
    }

    boolean isValueMissing() {
        return value == null;
    }

    long getTimestamp() {
        return timestamp;
    }

    private ByteBuffer getValue() throws IOException {
        if (isValueDeleted) {
            throw new IOException("Is not present");
        } else {
            return value;
        }
    }

    byte[] getBytes() throws IOException {
        final ByteBuffer buf = getValue().duplicate();
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    /**
     * Getting a Value with timestamp from bytes.
     *
     * @param bytes - byte array
     * @return Valye with timestamp.
     */
    public static Value getValueFromBytes(final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        final short isValueDeleted = buf.getShort();
        final boolean isDeleted;
        isDeleted = isValueDeleted == 1;
        final long timestamp = buf.getLong();
        return new Value(isDeleted, timestamp, buf);
    }

    /**
     * Creating a byte array from Value with timestamp.
     *
     * @return byte array
     */
    public byte[] getBytesFromValue() {
        short isDeleted;
        isDeleted = isValueDeleted ? (short) 1 : (short) -1;

        return ByteBuffer.allocate(Short.BYTES + Long.BYTES + value.remaining())
                .putShort(isDeleted)
                .putLong(timestamp)
                .put(value.duplicate()).array();
    }
}
