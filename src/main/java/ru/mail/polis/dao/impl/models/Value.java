package ru.mail.polis.dao.impl.models;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Value {

    private final long timestamp;
    @NotNull
    private final ByteBuffer data;

    /**
     * Creates value from Bytebuffer.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     */
    public Value(final long timestamp, @NotNull final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    /**
     * Creates empty value.
     *
     * @param timestamp that represents time of creation
     */
    public Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    public static Value of(final ByteBuffer data) {
        return new Value(System.currentTimeMillis(), data.duplicate().asReadOnlyBuffer());
    }

    public boolean isTombstone() {
        return data == null;
    }

    @NotNull
    public ByteBuffer getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
