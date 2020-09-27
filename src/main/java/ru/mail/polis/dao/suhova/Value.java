package ru.mail.polis.dao.suhova;

import java.nio.ByteBuffer;

public class Value {
    private final ByteBuffer data;
    private final long version;

    /**
     * Value from {@link Cell}.
     *
     * @param data    - content
     * @param version - timestamp
     */
    public Value(final ByteBuffer data, final long version) {
        this.data = data;
        this.version = version;
    }

    public static Value tombstone(final long version) {
        return new Value(null, version);
    }

    public ByteBuffer getData() {
        return data;
    }

    public boolean isTombstone() {
        return data == null;
    }

    public long getVersion() {
        return version;
    }
}
