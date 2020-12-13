package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer content;
    private final long expires;

    /**
     * Constructor for alive Cell.
     *
     * @param timestamp - initialization time
     * @param content - value
     * @param expires - time to live
     */
    public Value(final long timestamp, @NotNull final ByteBuffer content, final long expires) {
        this.timestamp = timestamp;
        this.content = content;
        this.expires = expires;
    }

    /**
     * Constructor for dead Cell.
     *
     * @param timestamp - negative value, means tombstone
     */
    public Value(final long timestamp) {
        this.timestamp = timestamp;
        this.content = null;
        this.expires = 0;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ByteBuffer getContent() {
        return content;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return Long.compare(o.timestamp, timestamp);
    }

    public boolean isTombstone() {
        return content == null;
    }
}
