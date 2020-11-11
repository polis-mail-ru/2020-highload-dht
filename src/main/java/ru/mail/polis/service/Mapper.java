package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Mapper {

    private Mapper() {

    }

    /**
     * ByteBuffer to byte array.
     *
     * @param buffer - input buffer.
     * @return byte array.
     */
    @NotNull
    public static byte[] toBytes(final ByteBuffer buffer) {
        final int oldPosition = buffer.position();
        final byte[] bytes = new byte[buffer.limit()];
        buffer.rewind();
        buffer.get(bytes);
        buffer.position(oldPosition);
        return bytes;
    }

    /**
     * Create a byte buffer from byte array.
     *
     * @param bytes - input byte array.
     * @return - created byte buffer.
     */
    public static ByteBuffer fromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
}
