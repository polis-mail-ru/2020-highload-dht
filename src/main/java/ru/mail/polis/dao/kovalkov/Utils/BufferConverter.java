package ru.mail.polis.dao.kovalkov.Utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferConverter {
    public static byte[] decompressKey(@NotNull final ByteBuffer key) {
        final byte[] bytes = unfoldToBytes(key);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] -= Byte.MIN_VALUE;
        }
        return bytes;
    }

    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
        final byte[] bytes = new byte[b.limit()];
        b.get(bytes).clear();
        return bytes;
    }


    public static ByteBuffer foldToBuffer(@NotNull final byte[] key) {
        final byte[] copy = Arrays.copyOf(key, key.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(copy);
    }
}
