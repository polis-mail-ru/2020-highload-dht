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
    private byte[] key;
    private byte[] value;
    private Date timestamp;
    private RecordState state;

    private Record() {
        /* nothing */
    }

    /**
     * Create record by key and value.
     *
     * @param key - record key.
     * @param value - record value.
     * @return - generated record.
     */
    public static Record newRecord(final String key, final byte[] value) {
        final var record = new Record();
        record.key = key.getBytes(StandardCharsets.UTF_8);
        record.value = Arrays.copyOf(value, value.length);
        record.state = RecordState.UNDEFINED;
        record.timestamp = new Date();
        return record;
    }

    /**
     * Create record with information, that this record was removed.
     *
     * @param key - the key of the record, which was removed.
     * @return - generated record.
     */
    public static Record newRemoved(final String key) {
        final var record = new Record();
        record.key = key.getBytes(StandardCharsets.UTF_8);
        record.value = new byte[]{};
        record.state = RecordState.REMOVED;
        record.timestamp = new Date();
        return record;
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
        final var record = new Record();
        record.key = key.getBytes(StandardCharsets.UTF_8);
        record.value = new byte[]{};
        record.timestamp = new Date(0);
        record.state = RecordState.PRESENTED;
        final var keyAsByteBuffer = ByteBufferUtils.toByteBuffer(record.key);
        final ByteBuffer response;
        try {
            response = dao.get(keyAsByteBuffer);
        } catch (NoSuchElementException ex) {
            record.state = RecordState.NOT_FOUND;
            return record;
        }
        if (RecordState.values()[response.get()] == RecordState.REMOVED) {
            record.state = RecordState.REMOVED;
        }
        record.timestamp = new Date(response.getLong());
        record.value = new byte[response.remaining()];
        response.get(record.value);
        return record;
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
        final var record = new Record();
        final var bb = ByteBuffer.wrap(rawValue);
        record.state = RecordState.values()[bb.get()];
        record.key = null;
        record.timestamp = new Date(bb.getLong());
        record.value = new byte[bb.remaining()];
        bb.get(record.value);
        return record;
    }

    /**
     * Save the record to DAO.
     *
     * @param dao - dao implementation.
     * @throws IOException - when record cannot be saved due to IO problems.
     */
    public void save(final DAO dao) throws IOException {
        dao.upsert(ByteBuffer.wrap(key), combineValue());
    }

    private ByteBuffer combineValue() {
        final int byteSize = 1;
        final int longSize = 8;
        final int valueSize = this.value.length;
        final var rawValue = ByteBuffer.allocate(byteSize + longSize + valueSize);
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
        final var rawValue = combineValue();
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