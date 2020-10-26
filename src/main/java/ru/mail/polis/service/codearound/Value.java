package ru.mail.polis.service.codearound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class Value {

    private static final Logger LOGGER = LoggerFactory.getLogger(Value.class);

    private final boolean isValueDeleted;
    private final long timestamp;
    private final ByteBuffer buf;

    /**
     * class instance const.
     *
     * @param isValueDeleted - evaluated to be true if value isn't obtainable on specific node replica
     *                       after deletion committed
     * @param timestamp - timestamp to trace back value modification moment
     * @param buf - some value
     */
    private Value(final boolean isValueDeleted,
                  final long timestamp,
                  final ByteBuffer buf) {

        this.isValueDeleted = isValueDeleted;
        this.timestamp = timestamp;
        this.buf = buf;
    }

    /**
     * resolves handling value as an existing one.
     *
     * @param buf - some value
     * @param timestamp - timestamp to trace back value modification moment
     * @return Value instance
     */
    public static Value resolveExistingValue(final ByteBuffer buf, final long timestamp) {
        return new Value(false, timestamp, buf);
    }

    /**
     * resolves handling value as a deleted one.
     *
     * @param timestamp - timestamp to trace back value modification moment
     * @return Value instance
     */
    public static Value resolveDeletedValue(final long timestamp) {
        return new Value(true, timestamp, ByteBuffer.allocate(0));
    }

    /**
     * resolves handling value as a missing one.
     *
     * @return Value instance
     */
    static Value resolveMissingValue() {
        return new Value(false, -1, null);
    }

    /**
     * evaluates target record status (whether deleted or not).
     *
     * @return true if actual status is the same as 'deleted' (based on isValueDeleted = true)
     */
    boolean isValueDeleted() {
        return isValueDeleted;
    }

    /**
     * evaluates target record status (whether missing or not).
     *
     * @return true if actual status is the same as 'missing' (based on value = null)
     */
    boolean isValueMissing() {
        return buf == null;
    }

    /**
     * timestamp getter.
     *
     * @return timestamp
     */
    long getTimestamp() {
        return timestamp;
    }

    /**
     * retrieves value written to ByteBuffer instance unless it's got removed at some moment.
     *
     * @return value written to ByteBuffer instance
     */
    private ByteBuffer getValue() throws IOException {
        if (isValueDeleted) {
            LOGGER.info("Target record has been removed");
            throw new IOException();
        } else {
            return buf;
        }
    }

    /**
     * retrieves value written to byte array.
     *
     * @return byte array
     */
    byte[] getBytes() throws IOException {
        final ByteBuffer buf = getValue().duplicate();
        final byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    /**
     * retrieves value with specific timestamp attribute.
     *
     * @param bytes - value written to byte array
     * @return timestamp-exposing Value instance
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
     * retrieves value written to byte array with specific timestamp attribute.
     *
     * @return timestamp-exposing value written to byte array
     */
    public byte[] getBytesFromValue() {
        short isDeleted;
        isDeleted = isValueDeleted ? (short) 1 : (short) -1;

        return ByteBuffer.allocate(Short.BYTES + Long.BYTES + buf.remaining())
                .putShort(isDeleted)
                .putLong(timestamp)
                .put(buf.duplicate()).array();
    }
}
