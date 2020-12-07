package ru.mail.polis.dao.impl.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Value {

    private final long timestamp;
    @Nullable
    private final ByteBuffer data;
    @NotNull
    private final Instant expire;

    /**
     * Creates value from Bytebuffer.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     * @param expire - given Instant
     */
    public Value(final long timestamp,
                 @Nullable final ByteBuffer data,
                 @NotNull final Instant expire) {
        this.timestamp = timestamp;
        this.data = data;
        this.expire = expire;
    }

    /**
     * Creates empty value.
     *
     * @param timestamp that represents time of creation
     */
    public Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
        this.expire = Instant.MAX;
    }

    /**
     * Creates value with no expire.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     */
    public Value(final long timestamp,
                 @Nullable final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
        this.expire = Instant.MAX;
    }

    public boolean isTombstone() {
        return data == null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expire);
    }

    public ByteBuffer getData() {
        return data == null ? null : data.duplicate();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NotNull
    public Instant getExpire() {
        return expire;
    }
}
