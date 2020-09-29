package ru.mail.polis.dao.kovalkov.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class BufferConverter {

    private BufferConverter() {
    }

//    /**
//     * Unfolding ByteBuffer to byte array.
//     * @param b - buffer
//     * @return - byte
//     */
//    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
//        final byte[] bytes = new byte[b.limit()];
//        b.get(bytes);
//        b.clear();
//        return bytes;
//    }
    public static ByteBuffer fromBytesToBuffer(@NotNull final byte[] key) {
        final byte[] copy = Arrays.copyOf(key, key.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(copy);
    }

    public static byte[] formBufferConverter(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] array = new byte[copy.remaining()];
        copy.get(array);
        return array;
    }

    public static byte[] unfoldToBytes(@NotNull final ByteBuffer key) {
        final byte[] arrayKey = formBufferConverter(key);
        for (int i = 0; i < arrayKey.length; i++) {
            arrayKey[i] -= Byte.MIN_VALUE;
        }
        return arrayKey;
    }
}
