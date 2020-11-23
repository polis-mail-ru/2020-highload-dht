package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {

    public static final long NEVER_EXPIRES = Long.MAX_VALUE;

    private final long timestamp;
    private final long expiresTimestamp;
    private final ByteBuffer data;

    /**
     * Creates the Value instance with expiresTimestamp.
     * Creates the tombstone if data is null
     */
    public Value(final long timestamp, final long expiresTimestamp, final ByteBuffer data) {
        assert timestamp >= 0L;
        assert expiresTimestamp >= 0L || expiresTimestamp == NEVER_EXPIRES;
        this.timestamp = timestamp;
        this.expiresTimestamp = expiresTimestamp;
        this.data = data;
    }

    /**
     * Creates the Value instance, that will never expire.
     * Creates the tombstone if data is null
     */
    public Value(final long timestamp, final ByteBuffer data) {
        assert timestamp >= 0L;
        this.timestamp = timestamp;
        this.expiresTimestamp = NEVER_EXPIRES;
        this.data = data;
    }

    public boolean isTombstone() {
        return data == null || isExpired();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresTimestamp;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(this.timestamp, o.timestamp);
    }

    @Override
    public int hashCode() {
        if (isTombstone()) {
            return Long.hashCode(timestamp);
        } else {
            return Long.hashCode(timestamp) + data.hashCode();
        }
    }

    public ByteBuffer getData() {
        assert !isTombstone();
        return data.duplicate();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExpiresTimestamp() {
        return expiresTimestamp;
    }

    @Override
    public String toString() {
        return "Value(timestamp = " + timestamp + "; expiresTimestamp = " + expiresTimestamp + "; data = " + data + ")";
    }
}
