package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {

    private final long timestamp;
    private final ByteBuffer data;

    /**
     * Creates the Value instance.
     * Creates the tombstone if data is null
     */
    public Value(final long timestamp, final ByteBuffer data) {
        assert timestamp >= 0L;
        this.timestamp = timestamp;
        this.data = data;
    }

    public boolean isTombstone() {
        return data == null;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(this.timestamp, o.timestamp);
    }

    @Override
    public int hashCode() {
        if (isTombstone()) {
            return Long.hashCode(timestamp);
        } else {
            return Long.hashCode(timestamp) + data.hashCode();
        }
    }

    public ByteBuffer getData() {
        assert !isTombstone();
        return data.duplicate();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
