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
        if (data == null) {
            this.data = null;
        } else {
            this.data = data.duplicate();
        }
        this.version = version;
    }

    public static Value tombstone(final long version) {
        return new Value(null, version);
    }

    public ByteBuffer getData() {
        if (data == null){
            return null;
        }
        return data.duplicate();
    }

    public boolean isTombstone() {
        return data == null;
    }

    public long getVersion() {
        return version;
    }
}
