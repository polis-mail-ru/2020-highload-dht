package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

public class TimestampRecord {
    private final long ts;
    private final ByteBuffer value;
    private final RecordType recordType;

    private enum RecordType {
        VALUE((byte) 1),
        DELETED((byte) -1),
        ABSENT((byte) 0);

        final byte value;

        RecordType(final byte value) {
            this.value = value;
        }

        static RecordType fromValue(final byte value) {
            if (value == VALUE.value) {
                return VALUE;
            } else if (value == DELETED.value) {
                return DELETED;
            } else {
                return ABSENT;
            }
        }
    }

    /**
     * Create the record.
     *
     * @param timestamp - define the time
     * @param value     - ByteBuffer
     * @param type      - RecordType
     */
    public TimestampRecord(final long timestamp,
                           final ByteBuffer value,
                           final RecordType type) {
        this.ts = timestamp;
        this.recordType = type;
        this.value = value;
    }

    public static TimestampRecord getEmpty() {
        return new TimestampRecord(-1, null, RecordType.ABSENT);
    }

    /**
     * Convert the record from bytes.
     *
     * @param bytes - array of byte
     * @return timestamp record instance
     */
    public static TimestampRecord fromBytes(@Nullable final byte[] bytes) {
        if (bytes == null) {
            return new TimestampRecord(-1, null, RecordType.ABSENT);
        }
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final TimestampRecord.RecordType recordType = RecordType.fromValue(buffer.get());
        final long ts = buffer.getLong();
        return new TimestampRecord(ts, buffer, recordType);
    }

    /**
     * Convert the record.
     *
     * @return timestamp record instance as bytes
     */
    public byte[] toBytes() {
        var valueLength = 0;
        if (isValue()) {
            valueLength = value.remaining();
        }
        final ByteBuffer byteBuff = ByteBuffer.allocate(1 + Long.BYTES + valueLength);
        byteBuff.put(recordType.value);
        byteBuff.putLong(getTimestamp());
        if (isValue()) {
            byteBuff.put(value.duplicate());
        }
        byteBuff.rewind();
        final byte[] result = new byte[byteBuff.remaining()];
        byteBuff.get(result);
        return result;
    }

    public static TimestampRecord fromValue(@NotNull final ByteBuffer value,
                                            final long timestamp) {
        return new TimestampRecord(timestamp, value, RecordType.VALUE);
    }

    public static boolean isEmptyRecord(@NotNull final byte[] bytes) {
        return bytes[0] != RecordType.VALUE.value;
    }

    public static TimestampRecord tombstone(final long timestamp) {
        return new TimestampRecord(timestamp, null, RecordType.DELETED);
    }

    public long getTimestamp() {
        return ts;
    }

    public boolean isValue() {
        return recordType == RecordType.VALUE;
    }

    public boolean isEmpty() {
        return recordType == RecordType.ABSENT;
    }

    public boolean isDeleted() {
        return recordType == RecordType.DELETED;
    }

    /**
     * Get the value only.
     *
     * @return value of the timestamp instance
     */
    public ByteBuffer getValue() {
        return value;
    }

    /**
     * Get the value only as bytes.
     *
     * @return value of the timestamp instance as bytes
     */
    public byte[] getValueAsBytes() {
        final ByteBuffer val = getValue().duplicate();
        final byte[] ret = new byte[val.remaining()];
        val.get(ret);
        return ret;
    }

    /**
     * Merge multiple records into one according to their timestamps.
     *
     * @param responses - to define the input
     * @return latest timestamp record instance
     */
    public static TimestampRecord merge(final List<TimestampRecord> responses) {
        if (responses.size() == 1) return responses.get(0);
        else {
            return responses.stream()
                    .filter(timestampRecord -> !timestampRecord.isEmpty())
                    .max(Comparator.comparingLong(TimestampRecord::getTimestamp))
                    .orElseGet(TimestampRecord::getEmpty);
        }
    }
}
