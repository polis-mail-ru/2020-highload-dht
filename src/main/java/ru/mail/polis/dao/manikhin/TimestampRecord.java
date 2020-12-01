package ru.mail.polis.dao.manikhin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.service.manikhin.Utils;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;

public class TimestampRecord {
    private final long timestamp;
    private final ByteBuffer value;
    private final RecordType recordType;
    private static final Logger log = LoggerFactory.getLogger(TimestampRecord.class);

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
     * @param type      - Type of record
     */
    public TimestampRecord(final long timestamp,
                           final ByteBuffer value,
                           final RecordType type) {

        this.timestamp = timestamp;
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

        log.debug("bytes length: " + String.valueOf(bytes.length));
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
        int valueLength = 0;

        if (isValue()) {
            valueLength = value.remaining();
        }

        final ByteBuffer byteBuff = ByteBuffer.allocate(1 + Long.BYTES + valueLength);
        byteBuff.put(recordType.value);
        byteBuff.putLong(getTimestamp());

        if (isValue()) {
            byteBuff.put(value.duplicate());
        }

        return byteBuff.array();
    }

    public static TimestampRecord fromValue(@NotNull final ByteBuffer value, final long timestamp) {
        return new TimestampRecord(timestamp, value, RecordType.VALUE);
    }

    public static TimestampRecord tombstone(final long timestamp) {
        return new TimestampRecord(timestamp, null, RecordType.DELETED);
    }

    public long getTimestamp() {
        return timestamp;
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
    public ByteBuffer getValue() throws NoSuchElementException {
        if (!isValue()) {
            throw new NoSuchElementException("Error to get value from TimestampRecord!");
        }
        return value;
    }

    /**
     * Get the value only as bytes.
     *
     * @return value of the timestamp instance as bytes
     */
    public byte[] getValueAsBytes() throws NoSuchElementException {
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
    public static TimestampRecord merge(final Collection<TimestampRecord> responses) {
        return responses.stream().filter(timestampRecord -> !timestampRecord.isEmpty())
                    .max(Comparator.comparingLong(TimestampRecord::getTimestamp))
                    .orElseGet(TimestampRecord::getEmpty);
    }
}
