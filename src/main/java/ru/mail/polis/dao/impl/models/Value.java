package ru.mail.polis.dao.impl.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public final class Value {

    private final long timestamp;
    @Nullable
    private final ByteBuffer data;

    /**
     * Creates value from Bytebuffer.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     */
    public Value(final long timestamp,
                 @Nullable final ByteBuffer data) {
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

    public static Value of(@NotNull final ByteBuffer data) {
        return new Value(System.currentTimeMillis(), data.duplicate().asReadOnlyBuffer());
    }

    public boolean isTombstone() {
        return data == null;
    }

    public ByteBuffer getData() {
        return data == null ? null : data.duplicate().asReadOnlyBuffer();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
