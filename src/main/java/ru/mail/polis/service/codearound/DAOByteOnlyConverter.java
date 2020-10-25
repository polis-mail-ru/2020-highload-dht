package ru.mail.polis.service.codearound;

import java.nio.ByteBuffer;

import static java.lang.Byte.MIN_VALUE;

public final class DAOByteOnlyConverter {

    private DAOByteOnlyConverter() {

    }

    /**
     * applies size manipulation to ensure proper conversion from ByteBuffer object to plain byte array.
     *
     * @param buf ByteBuffer object to be arithmetically tuned to byte array
     * @return a byte array object
     */
    public static byte[] tuneBufToArray(final ByteBuffer buf) {
        final byte[] byteArray = readByteArray(buf);

        for (int x = 0; x < byteArray.length; x++) {
            byteArray[x] -= MIN_VALUE;
        }
        return byteArray;
    }

    /**
     * applies size manipulation to ensure proper conversion from byte array to ByteBuffer instance.
     *
     * @param byteArray byte array to be arithmetically tuned to ByteBuffer instance
     * @return ByteBuffer object
     */
    public static ByteBuffer tuneArrayToBuf(final byte[] byteArray) {
        final byte[] dupArray = byteArray.clone();
        for (int x = 0; x < byteArray.length; x++) {
            dupArray[x] += MIN_VALUE;
        }
        return ByteBuffer.wrap(dupArray);
    }

    /**
     * converts ByteBuffer-defined value to byte array.
     *
     * @param buf ByteBuffer object to be handled to return byte array.
     * @return byte array object
     */
    public static byte[] readByteArray(final ByteBuffer buf) {
        final ByteBuffer dupBuffer = buf.duplicate();
        final byte[] vals = new byte[dupBuffer.remaining()];
        dupBuffer.get(vals);
        return vals;
    }
}
