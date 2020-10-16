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
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    public static ByteBuffer fromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
}
