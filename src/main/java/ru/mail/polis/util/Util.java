package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Util {
    /**
     * This converts ByteBuffer to byte array.
     *
     * @param buffer byte buffer
     * @return array bytes
     */
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);
        return arr;
    }
}
