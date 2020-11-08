package ru.mail.polis.dao;

import ru.mail.polis.service.ivanovandrey.Util;

import java.nio.ByteBuffer;

public class Timestamp {
    public enum State {
        DATA((byte) 0), DELETED((byte) -1), UNKNOWN((byte) -2);
        private final byte code;

        State(final byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        private static State fromCode(final byte code) {
            switch (code) {
                case 0:
                    return DATA;
                case -1:
                    return DELETED;
                default:
                    return UNKNOWN;
            }
        }
    }

    private final byte[] data;
    private final long timestampValue;
    private final State state;

    /**
     * Create a Timestamp instance.
     *
     * @param data - data stored in database.
     * @param timestampValue - timestamp of data updating.
     * @param state - correct data or deleted data.
     */
    public Timestamp(final byte[] data, final long timestampValue, final State state) {
        if (data == null) {
            this.data = new byte[0];
        } else {
            this.data = data.clone();
        }
        this.timestampValue = timestampValue;
        this.state = state;
    }

    /**
     * Pack timestampValue, state and data.
     */
    public byte[] getTimestampData() {
        final ByteBuffer buffer;
        buffer = ByteBuffer.allocate(data.length + Long.BYTES + Byte.BYTES);
        buffer.mark();
        buffer.putLong(timestampValue);
        buffer.put(state.getCode());
        buffer.put(data);
        buffer.reset();
        return Util.fromByteBufferToByteArray(buffer);
    }

    /**
     * Create a timestampValue.
     *
     * @param buffer - packed timestampValue, state and data.
     */
    public static Timestamp getTimestampByData(final ByteBuffer buffer) {
        final long timestamp = buffer.getLong();
        final byte state = buffer.get();
        return new Timestamp(Util.fromByteBufferToByteArray(buffer),
                timestamp,
                State.fromCode(state));
    }

    public byte[] getData() {
        return data.clone();
    }

    public long getTimestampValue() {
        return timestampValue;
    }

    public State getState() {
        return state;
    }
}
