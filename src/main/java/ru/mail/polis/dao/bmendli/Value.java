package ru.mail.polis.dao.bmendli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {

    public static final int NO_EXPIRATION = 0;
    public static final int TOMBSTONE_EXPIRE_TIME_MS = 10_000;

    @Nullable
    private final ByteBuffer data;
    private final long timestamp;
    private final long expireTime;

    /**
     * Creates new data wrapper, data may be null - tombstone.
     *
     * @param data      - new data
     * @param timestamp - time when data was saved
     */
    private Value(@Nullable final ByteBuffer data, final long timestamp, final long expireTime) {
        this.data = data;
        this.timestamp = timestamp;
        this.expireTime = expireTime;
    }

    public static Value newInstance() {
        return new Value(null, System.currentTimeMillis(), NO_EXPIRATION);
    }

    public static Value newInstance(final long timestamp, final long expireTime) {
        return new Value(null, timestamp, expireTime);
    }

    public static Value newInstance(@Nullable final ByteBuffer data, final long timestamp, final long expireTime) {
        return new Value(data, timestamp, expireTime);
    }

    @Nullable
    public ByteBuffer getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public boolean isTombstone() {
        return data == null;
    }

    public boolean isExpired() {
        return expireTime != NO_EXPIRATION && timestamp + expireTime < System.currentTimeMillis();
    }

    public boolean isExpiredTombstone() {
        return isTombstone() && timestamp + TOMBSTONE_EXPIRE_TIME_MS < System.currentTimeMillis();
    }

    @Override
    public int compareTo(@NotNull final Value value) {
        return -Long.compare(timestamp, value.timestamp);
    }
}
