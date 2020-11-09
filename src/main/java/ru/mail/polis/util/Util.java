package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Util {
    private Util() {
        /* Add private constructor to prevent instantiation */
    }

    /**
     * This converts ByteBuffer to byte array.
     *
     * @param buffer ByteBuffer
     * @return array bytes
     */
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);
        return arr;
    }

    /**
     * This wraps byte array into ByteBuffer.
     *
     * @param arr byte array
     * @return ByteBuffer with shifted bytes
     */
    public static ByteBuffer fromShiftedArray(@NotNull final byte[] arr) {
        final byte[] copy = Arrays.copyOf(arr, arr.length);

        int i = 0;
        while (i < copy.length) {
            copy[i] = (byte) (Byte.toUnsignedInt(copy[i]) + Byte.MIN_VALUE);
            i++;
        }

        return ByteBuffer.wrap(copy);
    }

    /**
     * This takes array from a ByteBuffer and performs all bytes shift by MIN_VALUE.
     *
     * @param buffer ByteBuffer
     * @return array with shifted bytes
     */
    public static byte[] toShiftedArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);

        int i = 0;
        while (i < arr.length) {
            arr[i] = (byte) (Byte.toUnsignedInt(arr[i]) - Byte.MIN_VALUE);
            i++;
        }

        return arr;
    }

    /**
     * Wraps string to ByteBuffer.
     * @param id - string to wrap
     * @return ByteBuffer
     */
    public static ByteBuffer toByteBuffer(final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }
}
