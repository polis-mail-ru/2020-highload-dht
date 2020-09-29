package ru.mail.polis.dao.Mariarheon;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
public class ByteBufferUtils {
    /**
     * Make byte[] from ByteBuffer.
     *
     * @param buffer - ByteBuffer
     * @return byte[] from buffer
     */
    public static byte[] toArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer duplicate = buffer.duplicate();
        final byte[] body = new byte[duplicate.remaining()];

        duplicate.get(body);

        return body;
    }

    /**
     * Make ByteBuffer from byte[]
     *
     * @param array - byte array
     * @return buffer from byte[]
     */
    public static ByteBuffer toByteBuffer(final byte[] array){
        return ByteBuffer.wrap(array);
    }
}
