package ru.mail.polis.dao.boriskin;

import java.nio.ByteBuffer;

final class Bytes {
    private Bytes() {
        // do nothing
    }

    // для доступа к ByteBuffer по переданному int
    static ByteBuffer fromInt(final int value) {
        final ByteBuffer res = ByteBuffer.allocate(Integer.BYTES);
        res.putInt(value);
        res.rewind();
        return res;
    }

    // для доступа к ByteBuffer по переданному long
    static ByteBuffer fromLong(final long value) {
        final ByteBuffer res = ByteBuffer.allocate(Long.BYTES);
        res.putLong(value);
        res.rewind();
        return res;
    }
}
