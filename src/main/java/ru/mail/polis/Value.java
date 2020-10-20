package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Value {

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

    /**
     * Метод, возвращабщий read only data.
     * Если data == null, то IllegalArgumentException
     *
     * @return read only data
     */
    @NotNull
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalArgumentException("Removed");
        }
        return data.asReadOnlyBuffer();
    }

    public boolean isRemoved() {
        return data == null;
    }

    public long getTimeStamp() {
        return timestamp;
    }
}
