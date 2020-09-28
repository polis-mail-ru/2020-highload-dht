package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Util {
    /**
     * This converts ByteBuffer to byte array.
     *
     * @param buffer byte buffer
     * @return array bytes
     */
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        byte[] arr = new byte[copy.remaining()];
        copy.get(arr);
        return arr;
    }
}
