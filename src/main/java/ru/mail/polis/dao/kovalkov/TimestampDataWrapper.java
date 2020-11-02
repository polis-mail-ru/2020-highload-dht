package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TimestampDataWrapper {
    private final RecordState state;
    private final long timestamp;
    private final ByteBuffer buffer;

    private TimestampDataWrapper(final ByteBuffer buffer, final long timestamp, @NotNull final RecordState state) {
        this.timestamp = timestamp;
        this.buffer = buffer;
        this.state = state;
    }

    @NotNull
    public static TimestampDataWrapper wrapFromBytesAndGetOne(final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        final RecordState state = getTypeOfState(buf.get());
        final long ts = buf.getLong();
            return new TimestampDataWrapper(buf, ts, state);
    }

    private static RecordState getTypeOfState(final byte b) {
        return b == 1 ? RecordState.EXIST : b == 0 ? RecordState.EMPTY : RecordState.DELETED;
    }

    private static byte getByteOfState(@NotNull RecordState recordState) {
        return (byte) (recordState == RecordState.EXIST ? 1 : recordState == RecordState.EMPTY ? 0 : -1);
    }

    @NotNull
    public static TimestampDataWrapper getOne(@NotNull final ByteBuffer buffer, final long timestamp) {
        return new TimestampDataWrapper(buffer, timestamp, RecordState.EXIST);
    }

    @NotNull
    public static TimestampDataWrapper getEmptyOne() {
        return new TimestampDataWrapper(null, -1, RecordState.EMPTY);
    }

    @NotNull
    public static TimestampDataWrapper getDeletedOne(final long timestamp) {
        return new TimestampDataWrapper(null, timestamp, RecordState.DELETED);
    }

    @NotNull
    public byte[] getBytes() throws IOException {
        final ByteBuffer buffer = getValue().duplicate();
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @NotNull
    public ByteBuffer getValue() throws IOException {
        if (state == RecordState.EXIST) {
            return buffer;
        } else throw new IOException("No such value");
    }


    @NotNull
    public byte[] toBytes() {
        final int cap = state == RecordState.EXIST ? this.buffer.remaining() : 0;
        final ByteBuffer b = ByteBuffer.allocate(cap + 1 + Long.BYTES);
        b.put(getByteOfState(getState()));
        b.putLong(getTimestamp());
        if (getState() == RecordState.EXIST) {
            b.put(buffer.duplicate());
        }
        return b.array();
    }

    @NotNull
    public static TimestampDataWrapper getRelevantTs(@NotNull final List<TimestampDataWrapper> list) {
        return list.size() == 1 ? list.get(0) : list.stream().filter(i -> i.getState() != RecordState.EMPTY)
                .max(Comparator.comparingLong(TimestampDataWrapper::getTimestamp))
                .orElseGet(TimestampDataWrapper::getEmptyOne);
    }

    @NotNull
    public RecordState getState() {
        return state;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
