package ru.mail.polis.dao.dmrnz;

import java.nio.ByteBuffer;

final class Bytes {
    private Bytes() {
    }

    /**
     * Creates a new {@link ByteBuffer} with the byte size of int,
     * puts value in this buffer and then rewinds the buffer.
     *
     * @param value is written in the byte buffer
     * @return ByteBuffer with int value
     */
    static ByteBuffer fromInt(final int value) {
        return ByteBuffer
                .allocate(Integer.BYTES)
                .putInt(value)
                .rewind();
    }

    /**
     * Creates a new {@link ByteBuffer} with the byte size of long,
     * puts value in this buffer and then rewinds the buffer.
     *
     * @param value is written in the byte buffer
     * @return ByteBuffer with long value
     */
    static ByteBuffer fromLong(final long value) {
        return ByteBuffer
                .allocate(Long.BYTES)
                .putLong(value)
                .rewind();
    }
}
