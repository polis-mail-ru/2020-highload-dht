package ru.mail.polis.service.zvladn7.util;

import one.nio.http.Response;
import one.nio.util.Utf8;

import java.nio.ByteBuffer;

public final class Bytes {

    private Bytes() {
    }

    /**
     * Transform String to ByteBuffer.
     * @param str - String
     * @return ByteBuffer wrapped over String
     */
    public static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    /**
     * Transform byte[] to ByteBuffer.
     * @param arr - byte[]
     * @return ByteBuffer wrapped over byte[]
     */
    public static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    /**
     * Transform String to byte[].
     * @param str - String
     * @return byte[] which were stored in String
     */
    public static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    /**
     * Transform ByteBuffer to byte[].
     * @param value - ByteBuffer
     * @return byte[] which were stored in ByteBuffer
     */
    public static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }
        return Response.EMPTY;
    }

}
