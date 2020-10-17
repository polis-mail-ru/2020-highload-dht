package ru.mail.polis.dao.mariarheon;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ByteBufferUtils {
    private ByteBufferUtils() {
    }

    /**
     * Make byte[] from ByteBuffer.
     *
     * @param buffer - ByteBuffer.
     * @return byte[] from buffer.
     */
    @NotNull
    public static byte[] toArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer duplicate = buffer.duplicate();
        final byte[] body = new byte[duplicate.remaining()];

        duplicate.get(body);

        return body;
    }

    /**
     * Make ByteBuffer from byte[].
     *
     * @param array - byte array.
     * @return buffer from byte[].
     */
    @NotNull
    public static ByteBuffer toByteBuffer(final byte[] array) {
        return ByteBuffer.wrap(array);
    }

    /**
     * Make byte[] from ByteBuffer unsigned <-> signed.
     *
     * @param buffer - ByteBuffer.
     * @return byte[] from buffer.
     */
    @NotNull
    public static byte[] toArrayUnsigned(@NotNull final ByteBuffer buffer) {
        var res = toArray(buffer);
        for (int i = 0; i < res.length; i++) {
            res[i] ^= Byte.MIN_VALUE;
        }
        return res;
    }

    /**
     * Make ByteBuffer from byte[] unsigned <-> signed.
     *
     * @param array - byte array.
     * @return buffer from byte[].
     */
    @NotNull
    public static ByteBuffer toByteBufferUnsigned(final byte[] array) {
        final byte[] cpy = Arrays.copyOf(array, array.length);
        for (int i = 0; i < cpy.length; i++) {
            cpy[i] ^= Byte.MIN_VALUE;
        }
        return toByteBuffer(cpy);
    }
}
