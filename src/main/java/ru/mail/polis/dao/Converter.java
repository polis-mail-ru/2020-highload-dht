package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class Converter {
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
        final ByteBuffer copyBuffer = buffer.duplicate();
        final byte[] arr = new byte[copyBuffer.remaining()];
        copyBuffer.get(arr);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = toUnsignedByte(arr[i]);
        }
        return arr;
    }

    /**
     * Wrap byte array into ByteBuffer.
     *
     * @param arr - byte array
     */
    public static ByteBuffer fromArrayShifted(@NotNull final byte[] arr) {
        final byte[] copyArray = Arrays.copyOf(arr, arr.length);
        for (int i = 0; i < copyArray.length; i++) {
            copyArray[i] = fromUnsignedByte(copyArray[i]);
        }
        return ByteBuffer.wrap(copyArray);
    }

    private static byte toUnsignedByte(final byte b) {
        return (byte) (Byte.toUnsignedInt(b) - Byte.MIN_VALUE);
    }

    private static byte fromUnsignedByte(final byte b) {
        return (byte) (Byte.toUnsignedInt(b) + Byte.MIN_VALUE);
    }
}
