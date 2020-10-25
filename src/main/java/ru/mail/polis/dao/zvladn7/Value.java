package ru.mail.polis.dao.zvladn7;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.zvladn7.exceptions.DeletedValueException;

import java.nio.ByteBuffer;

public class Value implements Comparable<Value> {
    private final long timestamp;
    private final ByteBuffer data;

    Value(final long timestamp, final ByteBuffer data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    private Value(final long timestamp) {
        this.timestamp = timestamp;
        this.data = null;
    }

    static Value newTombstoneValue(final long timestamp) {
        return new Value(timestamp);
    }

    boolean isTombstone() {
        return data == null;
    }

    /**
     * Return value.
     * @return ByteBuffer represented a value for some key.
     * @throws DeletedValueException - if data is null, that means that it is tombstone
     */
    @NotNull
    public ByteBuffer getData() throws DeletedValueException {
        if (data == null) {
            throw new DeletedValueException("Value has been removed!");
        }
        return data.asReadOnlyBuffer();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return Long.compare(o.timestamp, timestamp);
    }
}
