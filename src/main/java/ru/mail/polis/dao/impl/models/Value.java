package ru.mail.polis.dao.impl.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.utils.ResponseUtils;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;

public final class Value {

    private final long timestamp;
    @Nullable
    private final ByteBuffer data;
    @NotNull
    private final ZonedDateTime expire;

    /**
     * Creates value from Bytebuffer.
     *
     * @param timestamp that represents time of creation
     * @param data buffer to get data to value
     */
    public Value(final long timestamp,
                 @Nullable final ByteBuffer data,
                 @NotNull final ZonedDateTime expire) {
        this.timestamp = timestamp;
        this.data = data;
        this.expire = expire;
    }

    /**
     * Creates empty value.
     *
     * @param timestamp that represents time of creation
     */
    public Value(final long timestamp,
                 @NotNull final ZonedDateTime expire) {
        this.timestamp = timestamp;
        this.data = null;
        this.expire = expire;
    }

    public static Value of(@NotNull final ByteBuffer data,
                           @NotNull final ZonedDateTime expire) {
        return new Value(System.currentTimeMillis(),
                data.duplicate().asReadOnlyBuffer(),
                expire);
    }

    public boolean isTombstone() {
        return data == null;
    }

    public boolean isExpired() {
        final String current = ZonedDateTime.now().format(ResponseUtils.expirationFormat);
        return (ZonedDateTime.parse(current, ResponseUtils.expirationFormat)).isAfter(expire);
    }

    public ByteBuffer getData() {
        return data == null ? null : data.duplicate().asReadOnlyBuffer();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NotNull
    public ZonedDateTime getExpire() {
        return expire;
    }
}
