package ru.mail.polis.dao.dariagap;

import ru.mail.polis.util.Util;

import java.nio.ByteBuffer;

public class Timestamp {
    public static final Byte STATE_DATA = 0;
    public static final Byte STATE_DELETED = -1;
    public static final Byte STATE_UNKNOWN = -2;

    private final byte[] data;
    private final Long timestamp;
    private final Byte state;

    /**
     * Create a timestamp instance with data, timestamp and data state.
     *
     * @param data - data stored in database
     * @param timestamp - timestamp of data updating
     * @param state - correct data or deleted data
     */
    public Timestamp(final byte[] data, final Long timestamp, final Byte state) {
        this.data = data;
        this.timestamp = timestamp;
        this.state = state;
    }

    /**
     * Pack timestamp, state and data to byte[].
     */
    public byte[] getTimestampData() {
        final ByteBuffer buffer;
        final Integer dataLength;
        if (isDataNotEmpty()) {
            dataLength = data.length;
        } else {
            dataLength = 0;
        }
        buffer = ByteBuffer.allocate(dataLength + Long.BYTES + Byte.BYTES);
        buffer.mark();
        buffer.putLong(timestamp);
        buffer.put(state);
        if (isDataNotEmpty())
            buffer.put(data);
        buffer.reset();
        return Util.byteBufferToBytes(buffer);
    }

    /**
     * Create a timestamp instance by packed timestamp, state and data.
     *
     * @param buffer - packed timestamp, state and data
     */
    public static Timestamp getTimestampByData(final ByteBuffer buffer) {
        final Long timestamp = buffer.getLong();
        final Byte state = buffer.get();
        return new Timestamp(Util.byteBufferToBytes(buffer),timestamp,state);
    }

    private Boolean isDataNotEmpty() {
        if (this.data == null)
            return false;
        return true;
    }

    public byte[] getData() {
        return data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Byte getState() {
        return state;
    }
}
