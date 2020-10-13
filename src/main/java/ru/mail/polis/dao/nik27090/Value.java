package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

final class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer content;

    public Value(final long timestamp, @NotNull final ByteBuffer content) {
        this.timestamp = timestamp;
        this.content = content;
    }

    public Value(final long timestamp) {
        this.timestamp = timestamp;
        this.content = null;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ByteBuffer getContent() {
        return content;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return Long.compare(o.timestamp, timestamp);
    }

    boolean isTombstone() {
        return content == null;
    }
}
