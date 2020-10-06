package ru.mail.polis.dao.igor_manikhin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ByteConvertor {

    public static byte[] toArray(@NotNull final ByteBuffer buffer)  {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }

    public static byte[] toUnsignedArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copyBuffer = buffer.duplicate();
        final byte[] arr = new byte[copyBuffer.remaining()];
        copyBuffer.get(arr);

        for (int i = 0; i < arr.length; i++) {
            arr[i] = toUnsignedByte(arr[i]);
        }

        return arr;
    }

    public static ByteBuffer fromUnsignedArray(@NotNull final byte[] arr) {
        final byte[] copyArray = Arrays.copyOf(arr, arr.length);

        for (int i = 0; i < copyArray.length; i++) {
            copyArray[i] = fromUnsignedByte(copyArray[i]);
        }

        return ByteBuffer.wrap(copyArray);
    }

    private static byte toUnsignedByte(final byte b) {
        return (byte) (b - Byte.MIN_VALUE);
    }

    private static byte fromUnsignedByte(final byte b) {
        return (byte) (b + Byte.MIN_VALUE);
    }

}
