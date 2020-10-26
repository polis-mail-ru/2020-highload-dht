package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer data;

    Value(final ByteBuffer data, final long timestamp) {
        assert timestamp >= 0;
        this.data = data;
        this.timestamp = timestamp;
    }

    Value(final ByteBuffer data) {
        this.data = data;
        this.timestamp = Time.currentTime();
    }

    Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    Value() {
        this(null);
    }

    public ByteBuffer getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isTombstone() {
        return data == null;
    }

    @Override
    public int compareTo(@NotNull final Value value) {
        return -Long.compare(timestamp, value.timestamp);
    }
}
