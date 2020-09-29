package ru.mail.polis.dao.stasmilke;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Optional;

final class Value implements Comparable<Value> {
    private final Optional<ByteBuffer> data;
    private final long timestamp;

    Value(@Nullable final ByteBuffer data, final long timestamp) {
        assert timestamp > 0L;
        this.data = Optional.ofNullable(data);
        this.timestamp = timestamp;
    }

    Value(final long timestamp) {
        assert timestamp > 0L;
        this.data = Optional.empty();
        this.timestamp = timestamp;
    }

    boolean isTombstone() {
        return data.isEmpty();
    }

    @NotNull
    ByteBuffer getData() {
        assert !isTombstone();
        return data.orElseThrow().asReadOnlyBuffer();
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return -Long.compare(timestamp, o.timestamp);
    }

    public long sizeInBytes() {
        return Long.BYTES + (data.isPresent() ? data.get().remaining() : 0L);
    }

    public long getTimestamp() {
        return timestamp;
    }
}
