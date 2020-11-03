package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

public class TimestampDataWrapper {
//    private final RecordState state;
    private final long timestamp;
    private final ByteBuffer buffer;
    private final boolean isDeleted;

    private TimestampDataWrapper(final ByteBuffer buffer, final long timestamp, final boolean isDeleted) {
        this.timestamp = timestamp;
        this.buffer = buffer;
//        this.state = state;
        this.isDeleted = isDeleted;
    }

    @NotNull
    public static TimestampDataWrapper wrapFromBytesAndGetOne(final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
//        final RecordState state = getTypeOfState(buf.get());
        final short d = buf.getShort();
        final boolean isDelete = d == 1;
        final long ts = buf.getLong();
            return new TimestampDataWrapper(buf, ts, isDelete);
    }

//    private static RecordState getTypeOfState(final byte b) {
//        return b == 1 ? RecordState.EXIST : b == 0 ? RecordState.EMPTY : RecordState.DELETED;
//    }

//    private static byte getByteOfState(@NotNull RecordState recordState) {
//        return (byte) (recordState == RecordState.EXIST ? 1 : recordState == RecordState.EMPTY ? 0 : -1);
//    }

    @NotNull
    public static TimestampDataWrapper getOne(@NotNull final ByteBuffer buffer, final long timestamp) {
        return new TimestampDataWrapper(buffer, timestamp, false);
    }

    @NotNull
    public static TimestampDataWrapper getEmptyOne() {
        return new TimestampDataWrapper(ByteBuffer.allocate(0), -1, false);
    }

    @NotNull
    public static TimestampDataWrapper getDeletedOne(final long timestamp) {
        return new TimestampDataWrapper(ByteBuffer.allocate(0), timestamp, true);
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
        if (!isDeleted) {
            return buffer;
        } else throw new IOException("No such value");
    }


    @NotNull
    public byte[] toBytes() {
        final short cap = (short) (isDeleted ?  1 : 0);
        final ByteBuffer b = ByteBuffer.allocate(Short.BYTES + Long.BYTES + buffer.remaining());
        b.putShort(cap);
        b.putLong(getTimestamp());
        b.put(buffer.duplicate());
        return b.array();
    }

    @NotNull
    public static TimestampDataWrapper getRelevantTs(@NotNull final List<TimestampDataWrapper> list) {
        return list.size() == 1 ? list.get(0) : list.stream().filter(i -> i.getBuffer() != null )
                .max(Comparator.comparingLong(TimestampDataWrapper::getTimestamp))
                .orElseGet(TimestampDataWrapper::getEmptyOne);
    }

    @NotNull
    public boolean getState() {
        return isDeleted;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
