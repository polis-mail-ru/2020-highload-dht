package ru.mail.polis.utils;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class ByteUtils {

    private ByteUtils() {
    }

    public static ByteBuffer fromInt(final int value) {
        return ByteBuffer.allocate(Integer.BYTES).rewind().putInt(value).rewind();
    }

    public static ByteBuffer fromLong(final long value) {
        return ByteBuffer.allocate(Long.BYTES).rewind().putLong(value).rewind();
    }

    @NotNull
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

}
