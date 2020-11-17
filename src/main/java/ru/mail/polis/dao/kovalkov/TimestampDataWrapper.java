package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

public final class TimestampDataWrapper {
    private final boolean isDelete;
    private final long timestamp;
    private final ByteBuffer buffer;

    private TimestampDataWrapper(final ByteBuffer buffer, final long timestamp, final boolean isDeleted) {
        this.timestamp = timestamp;
        this.buffer = buffer;
        this.isDelete = isDeleted;
    }

    @NotNull
    public static TimestampDataWrapper getOne(@NotNull final ByteBuffer buffer, final long timestamp) {
        return new TimestampDataWrapper(buffer, timestamp, false);
    }

    @NotNull
    public static TimestampDataWrapper getDeletedOne(final long timestamp) {
        return new TimestampDataWrapper(ByteBuffer.allocate(0), timestamp, true);
    }

    @NotNull
    public static TimestampDataWrapper getMissingOne() {
        return new TimestampDataWrapper(ByteBuffer.allocate(0), -1, false);
    }

    /**
     * get byte included timestamp.
     *
     * @return byte with timestamp.
     * @throws IOException may thrown if suck value does not exist.
     */
    @NotNull
    public byte[] getTSBytes() throws IOException {
        final ByteBuffer buf = getValue().duplicate();
        final var bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    @NotNull
    private ByteBuffer getValue() throws IOException {
        if (isDelete) throw new IOException("No such value");
        return buffer;
    }

    /**
     * Wrapped byte from DB and get new instance of wrapper.
     *
     * @param bytes data from DAO.
     * @return new instance of wrapper.
     */
    @NotNull
    public static TimestampDataWrapper wrapFromBytesAndGetOne(final byte[] bytes) {
        final var buf = ByteBuffer.wrap(bytes);
        final var isDel = buf.getShort() == 1;
        final var ts = buf.getLong();
        return new TimestampDataWrapper(buf, ts, isDel);
    }

    /**
     * Get byte from value and add some data about value
     *
     * @return byte arr include delete status, timestamp and value
     */
    @NotNull
    public byte[] toBytesFromValue() {
        final var cap = (short) (isDelete ? 1 : -1);
        final var buf = ByteBuffer.allocate(Short.BYTES + Long.BYTES + buffer.remaining());
        return buf.putShort(cap).putLong(timestamp).put(buffer.duplicate()).array();
    }

    /**
     * Filtered all val, and get most actual data sing max and comparing by timestamp.
     *
     * @param vals list of values.
     * @return most relevant value.
     */
    @NotNull
    public static TimestampDataWrapper getRelevantTs(@NotNull final List<TimestampDataWrapper> vals) {
        return vals.size() == 1 ? vals.get(0) : vals.stream().filter(v -> !v.isMissing())
                .max(Comparator.comparingLong(TimestampDataWrapper::getTimestamp))
                .orElseGet(TimestampDataWrapper::getMissingOne);
    }

    public boolean isDelete() {
        return isDelete;
    }

    public boolean isMissing() {
        return buffer == null;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
