package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static ru.mail.polis.Time.getCurrentTime;

public final class Value implements Comparable<Value> {

    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private final long timestamp;
    private final ByteBuffer data;

    Value(final long timestamp, final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    public static Value of(final ByteBuffer data) {
        return new Value(getCurrentTime(), data.duplicate());
    }

    public static Value tombstone() {
        return new Value(getCurrentTime(), null);
    }

    /**
     * Метод, возвращабщий read only data.
     * Если data == null, то IllegalArgumentException
     *
     * @return read only data
     */
    @NotNull
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalArgumentException("Removed");
        }
        return data.asReadOnlyBuffer();
    }

    public boolean isRemoved() {
        return data == null;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(timestamp, o.timestamp);
    }

    public long getTimeStamp() {
        return timestamp;
    }
}
