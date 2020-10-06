package ru.mail.polis.dao.manikhin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ByteConvertor {

    private ByteConvertor() {
    }

    /**
     * Converter ByteBuffer from java.nio to byte array.
     *
     * @param buffer byte buffer
     * @return array bytes
     */
    public static byte[] toArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }

    /**
     * Extract byte array from a Unsigned ByteBuffer.
     *
     * @param buffer ByteBuffer to extract from
     * @return array with unsigned bytes
     */
    public static byte[] toUnsignedArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copyBuffer = buffer.duplicate();
        final byte[] arr = new byte[copyBuffer.remaining()];
        copyBuffer.get(arr);

        for (int i = 0; i < arr.length; i++) {
            arr[i] = toUnsignedByte(arr[i]);
        }

        return arr;
    }

    /**
     * Convert byte array into ByteBuffer.
     *
     * @param arr byte array
     * @return ByteBuffer with unsigned bytes
     */
    public static ByteBuffer fromUnsignedArray(@NotNull final byte[] arr) {
        final byte[] copyArray = Arrays.copyOf(arr, arr.length);

        for (int i = 0; i < copyArray.length; i++) {
            copyArray[i] = fromUnsignedByte(copyArray[i]);
        }

        return ByteBuffer.wrap(copyArray);
    }

    public static byte toUnsignedByte(final byte b) {
        return (byte) (b - Byte.MIN_VALUE);
    }

    public static byte fromUnsignedByte(final byte b) {
        return (byte) (b + Byte.MIN_VALUE);
    }

}
