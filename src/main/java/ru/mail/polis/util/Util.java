package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Util {
    private Util() {

    }

    /**
     * Convert data from ByteBuffer to byte[].
     *
     * @param buffer data to convert
     * @return byte array
     */
    public static byte[] byteBufferToBytes(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufCopy = buffer.duplicate();
        final byte[] bytes = new byte[bufCopy.remaining()];
        bufCopy.get(bytes,0,bytes.length);
        bufCopy.clear();
        return bytes;
    }

    /**
     * Convert data from ByteBuffer to unsigned byte[].
     *
     * @param buffer data to convert
     * @return unsigned byte array
     */
    public static byte[] pack(@NotNull final ByteBuffer buffer) {
        byte[] bytes = byteBufferToBytes(buffer);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= Byte.MIN_VALUE;
        }
        return bytes;
    }

    /**
     * Convert data from unsigned byte[] to signed ByteBuffer.
     *
     * @param bytes data to convert
     * @return signed ByteBuffer
     */
    public static ByteBuffer unpack(@NotNull final byte[] bytes) {
        byte[] bytesCopy = bytes.clone();
        for (int i = 0; i < bytesCopy.length; i++) {
            bytesCopy[i] ^= Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(bytesCopy);
    }
}
