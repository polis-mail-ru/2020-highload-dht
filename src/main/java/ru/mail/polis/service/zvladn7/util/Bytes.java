package ru.mail.polis.service.zvladn7.util;

import one.nio.http.Response;
import one.nio.util.Utf8;

import java.nio.ByteBuffer;

public final class Bytes {

    private Bytes() {
    }

    public static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    public static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    public static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    public static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }
        return Response.EMPTY;
    }

}
