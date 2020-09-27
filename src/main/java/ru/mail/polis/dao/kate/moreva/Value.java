package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer data;

    Value(final ByteBuffer data, final long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }

    Value(final ByteBuffer data) {
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    Value() {
        this(null);
    }

    ByteBuffer getData() {
        return data;
    }

    long getTimestamp() {
        return timestamp;
    }

    boolean isTombstone() {
        return data == null;
    }

    @Override
    public int compareTo(@NotNull final Value value) {
        return -Long.compare(timestamp, value.timestamp);
    }
}
