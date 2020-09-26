package ru.mail.polis.dao.zvladn7;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer data;

    Value(final long timestamp, final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    public static Value newTombstoneValue(final long timestamp) {
        return new Value(timestamp);
    }

    boolean isTombstone() {
        return data == null;
    }

    @NotNull
    ByteBuffer getData() {
        if (data == null) {
            throw new NoSuchElementException("Value has been removed!");
        }
        return data.asReadOnlyBuffer();
    }

    @NotNull
    long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return Long.compare(o.timestamp, timestamp);
    }
}
