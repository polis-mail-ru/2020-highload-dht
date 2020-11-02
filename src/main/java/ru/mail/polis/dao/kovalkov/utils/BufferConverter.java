package ru.mail.polis.dao.kovalkov.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class BufferConverter {

    private BufferConverter() {
    }

    /**
     * Unfolding ByteBuffer to byte array.
     *
     * @param key - Buffer.
     * @return - Bytes.
     */
    @NotNull
    public static byte[] unfoldToBytes(@NotNull final ByteBuffer key) {
        final ByteBuffer copyBuffer = key.duplicate();
        final byte[] bytes = new byte[copyBuffer.remaining()];
        copyBuffer.get(bytes);
        return bytes;
    }

    /**
     * Convert ByteBuffer to byte array.
     *
     * @param key - Buffer.
     * @return - signed Bytes array.
     */
    @NotNull
    public static byte[] convertBytes(@NotNull final ByteBuffer key) {
        final byte[] arrayKey = unfoldToBytes(key);
        for (int i = 0; i < arrayKey.length; i++) {
            arrayKey[i] -= Byte.MIN_VALUE;
        }
        return arrayKey;
    }

    /**
     * Fold byte array to ByteBuffer.
     *
     * @param key - byte array.
     * @return - ByteBuffer.
     */
    @NotNull
    public static ByteBuffer foldToBuffer(@NotNull final byte[] key) {
        final byte[] copy = Arrays.copyOf(key, key.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(copy);
    }
}
