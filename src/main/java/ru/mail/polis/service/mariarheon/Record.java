package ru.mail.polis.service.mariarheon;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * Represents key-value pair for storing in RocksDB.
 */
public final class Record {
    private final byte[] key;
    private final byte[] value;
    private final Date timestamp;
    private final RecordState state;
    private static final RecordState[] recordStateValues = RecordState.values();

    private Record(final byte[] key, final byte[] value, final Date timestamp,
                   final RecordState state) {
        if (key == null) {
            this.key = null;
        } else {
            this.key = Arrays.copyOf(key, key.length);
        }
        this.value = Arrays.copyOf(value, value.length);
        this.timestamp = timestamp;
        this.state = state;
    }

    /**
     * Create record by key and value.
     *
     * @param key - record key.
     * @param value - record value.
     * @return - generated record.
     */
    public static Record newRecord(final String key, final byte[] value) {
        final var argKey = key.getBytes(StandardCharsets.UTF_8);
        final var argValue = Arrays.copyOf(value, value.length);
        final var argState = RecordState.UNDEFINED;
        final var argTimestamp = new Date();
        return new Record(argKey, argValue, argTimestamp, argState);
    }

    /**
     * Create record with information, that this record was removed.
     *
     * @param key - the key of the record, which was removed.
     * @return - generated record.
     */
    public static Record newRemoved(final String key) {
        final var argKey = key.getBytes(StandardCharsets.UTF_8);
        final var argValue = new byte[]{};
        final var argState = RecordState.REMOVED;
        final var argTimestamp = new Date();
        return new Record(argKey, argValue, argTimestamp, argState);
    }

    /**
     * Read record from DAO by key. If record does not exist, it will return
     * true for wasNotFound().
     *
     * @param dao - dao implementation.
     * @param key - key of the record.
     * @return - record, which was read.
     * @throws IOException - if record cannot be read due to problems with DAO.
     */
    public static Record newFromDAO(final DAO dao, final String key) throws IOException {
        final var argKey = key.getBytes(StandardCharsets.UTF_8);
        var argValue = new byte[]{};
        var argTimestamp = new Date(0);
        var argState = RecordState.PRESENTED;
        final var keyAsByteBuffer = ByteBufferUtils.toByteBuffer(argKey);
        final ByteBuffer response;
        try {
            response = dao.get(keyAsByteBuffer);
        } catch (NoSuchElementException ex) {
            argState = RecordState.NOT_FOUND;
            return new Record(argKey, argValue, argTimestamp, argState);
        }
        if (response.get() == RecordState.REMOVED.ordinal()) {
            argState = RecordState.REMOVED;
        }
        argTimestamp = new Date(response.getLong());
        argValue = new byte[response.remaining()];
        response.get(argValue);
        return new Record(argKey, argValue, argTimestamp, argState);
    }

    /**
     * Returns record with parsed information, including
     * value of the record, state (usual, removed, not found) of the record
     * and key = null.
     *
     * @param rawValue - raw (unparsed) value from record.
     * @return - record with parsed value information.
     */
    public static Record newFromRawValue(final byte[] rawValue) {
        return newFromRawValue(ByteBuffer.wrap(rawValue));
    }

    /**
     * Returns record with parsed information, including
     * value of the record, state (usual, removed, not found) of the record
     * and key = null.
     *
     * @param rawValue - raw (unparsed) value from record.
     * @return - record with parsed value information.
     */
    public static Record newFromRawValue(final ByteBuffer rawValue) {
        rawValue.mark();
        final var argState = recordStateValues[rawValue.get()];
        final var argTimestamp = new Date(rawValue.getLong());
        final var argValue = new byte[rawValue.remaining()];
        rawValue.get(argValue);
        rawValue.reset();
        return new Record(null, argValue, argTimestamp, argState);
    }

    /**
     * Save the record to DAO.
     *
     * @param dao - dao implementation.
     * @throws IOException - when record cannot be saved due to IO problems.
     */
    public void save(final DAO dao) throws IOException {
        dao.upsert(ByteBuffer.wrap(key), serialize());
    }

    private ByteBuffer serialize() {
        final int valueSize = this.value.length;
        final var rawValue = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + valueSize);
        final byte stateAsByte = (byte) this.state.ordinal();
        rawValue.put(stateAsByte);
        rawValue.putLong(this.timestamp.getTime());
        rawValue.put(this.value);
        rawValue.rewind();
        return rawValue;
    }

    /**
     * Returns raw value of the record, which includes information about
     * the state (usual, removed, not found) of the record, timestamp and the value.
     *
     * @return - raw value of the record.
     */
    public byte[] getRawValue() {
        final var rawValue = serialize();
        final var res = new byte[rawValue.remaining()];
        rawValue.get(res);
        return res;
    }

    /**
     * Returns true if record was not found.
     *
     * @return - true if record was not found.
     */
    public boolean wasNotFound() {
        return this.state == RecordState.NOT_FOUND;
    }

    /**
     * Returns true if record was removed.
     *
     * @return - true if record was removed.
     */
    public boolean isRemoved() {
        return this.state == RecordState.REMOVED;
    }

    /**
     * Returns the value of the record without state and timestamp information.
     *
     * @return - value of the record without meta-information.
     */
    public byte[] getValue() {
        return Arrays.copyOf(this.value, this.value.length);
    }

    /**
     * Returns the time of changing/creating/removing the record.
     *
     * @return - the time of changing/creating/removing the record.
     */
    public Date getTimestamp() {
        return this.timestamp;
    }
}
