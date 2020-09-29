package ru.mail.polis.dao.kovalkov.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class BufferConverter {

    private BufferConverter() {
    }

    /**
     * Unfolding ByteBuffer to byte array.
     *
     * @param key - Buffer.
     * @return - Bytes.
     */
    public static byte[] unfoldToBytes(@NotNull final ByteBuffer key) {
        final byte[] bytes = new byte[key.limit()];
        key.get(bytes);
        key.clear();
        return bytes;
    }
}
