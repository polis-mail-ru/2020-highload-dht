package ru.mail.polis.dao.kovalkov.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class BufferConverter {

    private BufferConverter() {
    }

    /**
     * Unfolding ByteBuffer to byte array.
     * @param b - buffer
     * @return - byte
     */
    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
        final byte[] bytes = new byte[b.limit()];
        b.get(bytes);
        b.clear();
        return bytes;
    }


}
