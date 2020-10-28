package ru.mail.polis.dao.kovalkov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TimestampDataWrapper {
    private static final Logger log = LoggerFactory.getLogger(TimestampDataWrapper.class);

    private final boolean deleteFlag;
    private final long time;
    private final ByteBuffer buffer;

    private TimestampDataWrapper(final boolean deleteFlag, final long timestamp, final ByteBuffer buffer) {
        this.deleteFlag = deleteFlag;
        this.time = timestamp;
        this.buffer = buffer;
    }

    public static TimestampDataWrapper resolveExistingValue(final ByteBuffer buffer, final long timestamp) {
        return new TimestampDataWrapper(false, timestamp, buffer);
    }

    public static TimestampDataWrapper resolveDeletedValue(final long timestamp) {
        return new TimestampDataWrapper(true, timestamp, ByteBuffer.allocate(0));
    }

    static TimestampDataWrapper resolveMissingValue() {
        return new TimestampDataWrapper(false, -1, null);
    }

    boolean isDeleteFlag() {
        return deleteFlag;
    }

    boolean isValueMissing() {
        return buffer == null;
    }

    long getTime() {
        return time;
    }

    private ByteBuffer getValue() throws IOException {
        if (deleteFlag) {
            log.info("Record has been removed");
            throw new IOException();
        } else {
            return buffer;
        }
    }
    public static TimestampDataWrapper composeFromBytes(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final short isValueDeleted = buffer.getShort();
        final long timestamp = buffer.getLong();

        return new TimestampDataWrapper(isValueDeleted == 1, timestamp, buffer);
    }

    public byte[] getValueBytes() {
        final short isDeleted = deleteFlag ? (short) 1 : (short) -1;
        return ByteBuffer.allocate(Short.BYTES + Long.BYTES + buffer.remaining())
                .putShort(isDeleted).putLong(time).put(buffer.duplicate()).array();
    }
}
