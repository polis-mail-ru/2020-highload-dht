package ru.mail.polis.dao.impl.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;

public final class Value {

    private final long timestamp;
    @Nullable
    private final ByteBuffer data;
    private final Instant expire;

    /**
     * Creates value from Bytebuffer.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     */
    public Value(final long timestamp,
                 @Nullable final ByteBuffer data,
                 final long seconds,
                 final int nanos) {
        this.timestamp = timestamp;
        this.data = data;
        this.expire = Instant.ofEpochSecond(seconds, nanos);
    }

    /**
     * Creates empty value.
     *
     * @param timestamp that represents time of creation
     */
    public Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
        this.expire = null;
    }

    public static Value of(@NotNull final ByteBuffer data,
                           final long seconds,
                           final int nanos) {
        return new Value(System.currentTimeMillis(),
                data.duplicate().asReadOnlyBuffer(),
                seconds,
                nanos);
    }

    public boolean isTombstone() {
        return data == null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(Objects.requireNonNull(expire));
    }

    public ByteBuffer getData() {
        return data == null ? null : data.duplicate().asReadOnlyBuffer();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Instant getExpire() {
        return expire;
    }
}
