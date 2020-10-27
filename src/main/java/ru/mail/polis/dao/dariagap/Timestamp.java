package ru.mail.polis.dao.dariagap;

import ru.mail.polis.util.Util;

import java.nio.ByteBuffer;

public class Timestamp {
    public static final Byte STATE_DATA = 0;
    public static final Byte STATE_DELETED = -1;
    public static final Byte STATE_UNKNOWN = -2;

    private final byte[] data;
    private final Long timestampValue;
    private final Byte state;

    /**
     * Create a Timestamp instance with data, timestampValue and data state.
     *
     * @param data - data stored in database
     * @param timestampValue - timestamp of data updating
     * @param state - correct data or deleted data
     */
    public Timestamp(final byte[] data, final Long timestampValue, final Byte state) {
        if (data != null) {
            this.data = data.clone();
        } else {
            this.data = null;
        }
        this.timestampValue = timestampValue;
        this.state = state;
    }

    /**
     * Pack timestampValue, state and data to byte[].
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
        buffer.putLong(timestampValue);
        buffer.put(state);
        if (isDataNotEmpty()) {
            buffer.put(data);
        }
        buffer.reset();
        return Util.byteBufferToBytes(buffer);
    }

    /**
     * Create a timestampValue instance by packed timestampValue, state and data.
     *
     * @param buffer - packed timestampValue, state and data
     */
    public static Timestamp getTimestampByData(final ByteBuffer buffer) {
        final Long timestamp = buffer.getLong();
        final Byte state = buffer.get();
        return new Timestamp(Util.byteBufferToBytes(buffer),timestamp,state);
    }

    private Boolean isDataNotEmpty() {
        if (this.data == null) {
            return false;
        }
        return true;
    }

    public byte[] getData() {
        if (isDataNotEmpty()) {
            return data.clone();
        }
        return null;
    }

    public Long getTimestampValue() {
        return timestampValue;
    }

    public Byte getState() {
        return state;
    }
}
