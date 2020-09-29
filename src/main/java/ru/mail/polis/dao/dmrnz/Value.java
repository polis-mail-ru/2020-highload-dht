package ru.mail.polis.dao.dmrnz;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static ru.mail.polis.dao.dmrnz.Time.currentTimeInNano;

final class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer data;

    Value(final long timestamp, final ByteBuffer data) {
        assert timestamp >= 0;
        this.timestamp = timestamp;
        this.data = data;
    }

    static Value of(final ByteBuffer data) {
        return new Value(currentTimeInNano(), data.duplicate());
    }

    static Value tombstone() {
        return new Value(currentTimeInNano(), null);
    }

    /**
     * Returns read-only data of {@link Value}.
     *
     * @throws IllegalArgumentException if data is null
     * @return Data as read-only ByteBuffer
     */
    @NotNull
    ByteBuffer getData() {
        if (data == null) {
            throw new IllegalArgumentException("Removed");
        }
        return data.asReadOnlyBuffer();
    }

    boolean isRemoved() {
        return data == null;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(timestamp, o.timestamp);
    }

    long getTimeStamp() {
        return timestamp;
    }
}
