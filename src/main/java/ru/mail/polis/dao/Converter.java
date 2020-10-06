package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class Converter {

    private Converter(){
    }

    /** Convert from ByteBuffer to Byte massive.
     *
     * @param buffer - ByteBuffer to convert
     */
    public static byte[] fromByteBufferToByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }

    /**
     * Extract array from a ByteBuffer and shift all bytes by min value.
     *
     * @param buffer - ByteBuffer to extract from
     */
    public static byte[] toArrayShifted(@NotNull final ByteBuffer buffer) {
        var res = fromByteBufferToByteArray(buffer);
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte)(Byte.toUnsignedInt(res[i]) - Byte.MIN_VALUE);
        }
        return res;
    }

    /**
     * Wrap byte array into ByteBuffer.
     *
     * @param arr - byte array
     */
    public static ByteBuffer fromArrayShifted(@NotNull final byte[] arr) {
        final byte[] cpy = Arrays.copyOf(arr, arr.length);
        for (int i = 0; i < cpy.length; i++) {
            cpy[i] = (byte) (Byte.toUnsignedInt(cpy[i]) + Byte.MIN_VALUE);
        }
        return ByteBuffer.wrap(cpy);
    }
}
