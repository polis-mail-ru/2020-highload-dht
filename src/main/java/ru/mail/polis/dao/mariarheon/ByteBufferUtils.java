package ru.mail.polis.dao.mariarheon;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

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
}
