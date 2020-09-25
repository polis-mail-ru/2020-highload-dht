package ru.mail.polis.dao.art241111;

import java.nio.ByteBuffer;

public class Value {
    private final ByteBuffer data;
    private final long version;

    Value(final ByteBuffer data, final long version) {
        this.data = data;
        this.version = version;
    }

    static Value tombstone(final long version) {
        return new Value(null, version);
    }

    public ByteBuffer getData() {
        return data;
    }

    boolean isTombstone() {
        return data == null;
    }

    public long getVersion() {
        return version;
    }
}
