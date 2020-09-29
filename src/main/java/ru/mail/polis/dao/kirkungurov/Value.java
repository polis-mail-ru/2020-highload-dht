package ru.mail.polis.dao.kirkungurov;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class Value implements Comparable<Value> {

    @Nullable
    private final ByteBuffer data;
    private final long timestamp;

    public Value(final long timestamp, @Nullable final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    public Value(@Nullable final ByteBuffer data) {
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }

    public Value(final long timestamp) {
        this.data = null;
        this.timestamp = timestamp;
    }

    public Value() {
        this(null);
    }

    public boolean isTombstone() {
        return data == null;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public ByteBuffer getData() {
        return data;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(timestamp, o.getTimestamp());
    }
}
