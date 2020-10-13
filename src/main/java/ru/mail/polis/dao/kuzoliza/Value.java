package ru.mail.polis.dao.kuzoliza;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {

    private final long timestamp;
    private final ByteBuffer data;

    Value(final long timestamp, @NotNull final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    long getTimestamp() {
        return timestamp;
    }

    ByteBuffer getData() {
        return data;
    }

    @Override
    public int compareTo(@NotNull final Value value) {
        return -Long.compare(timestamp, value.timestamp);
    }

    boolean isTombstone() {
        return data == null;
    }

}
