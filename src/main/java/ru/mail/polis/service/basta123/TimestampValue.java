package ru.mail.polis.service.basta123;

import java.nio.ByteBuffer;

public final class TimestampValue {

    private static final TimestampValue TIMESTAMP_VALUE = new TimestampValue(false, -1, ByteBuffer.allocate(0));
    private final ByteBuffer buffer;
    private final boolean isValueDeleted;
    private final long timestamp;

    /**
     * class const.
     *
     * @param isValueDeleted - value deleted or not.
     * @param timestamp      - timestamp to sync values.
     * @param buffer         - value.
     */
    public TimestampValue(final boolean isValueDeleted,
                          final long timestamp,
                          final ByteBuffer buffer) {
        this.isValueDeleted = isValueDeleted;
        this.timestamp = timestamp;
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer.duplicate();
    }

    static TimestampValue getTimestampValue() {
        return TIMESTAMP_VALUE;
    }

    boolean isValueDeleted() {
        return isValueDeleted;
    }

    boolean valueExists() {
        return buffer == null;
    }

    long getTimeStamp() {
        return timestamp;
    }

    /**
     * get value from bytes.
     *
     * @param bytes - has value and timestamp
     * @return TimestampValue - leep value and timestamp
     */
    public static TimestampValue getTimestampValueFromBytes(final byte[] bytes) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        final short isValueDeleted = byteBuffer.getShort();
        final boolean isDeleted;
        if (isValueDeleted == 1) {
            isDeleted = true;
        } else {
            isDeleted = false;
        }
        final long timestamp = byteBuffer.getLong();
        return new TimestampValue(isDeleted, timestamp, byteBuffer);
    }

    /**
     * get bytes from timestamp value.
     *
     * @param isValueDeleted - value deleted or not
     * @param timestamp - time when value was added/deleted
     * @param buffer - keep in inself value
     * @return timestamp-exposing value written to byte array
     */
    public static byte[] getBytesFromTimestampValue(final boolean isValueDeleted,
                                                    final long timestamp,
                                                    final ByteBuffer buffer) {
        short isDeleted;
        if (isValueDeleted) {
            isDeleted = (short) 1;
        } else {
            isDeleted = (short) -1;
        }
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Short.BYTES + Long.BYTES + buffer.remaining());
        return byteBuffer
                .putShort(isDeleted)
                .putLong(timestamp)
                .put(buffer.duplicate())
                .array();

    }
}
