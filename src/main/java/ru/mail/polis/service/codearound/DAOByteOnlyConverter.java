package ru.mail.polis.service.codearound;

import java.nio.ByteBuffer;

import static java.lang.Byte.MIN_VALUE;

public class DAOByteOnlyConverter {

    public static byte[] tuneBufToArray(final ByteBuffer buf) {
            final byte[] byteArray = readByteArray(buf);

            for (int x = 0; x < byteArray.length; x++) {
                byteArray[x] -= MIN_VALUE;
            }
            return byteArray;
    }

    public static ByteBuffer tuneArrayToBuf(final byte[] byteArray) {
        final byte[] dupArray = byteArray.clone();
        for (int x = 0; x < byteArray.length; x++) {
            dupArray[x] += MIN_VALUE;
        }
        return ByteBuffer.wrap(dupArray);
    }

    public static byte[] readByteArray(final ByteBuffer buf) {
        final ByteBuffer dupBuffer = buf.duplicate();
        final byte[] vals = new byte[dupBuffer.remaining()];
        dupBuffer.get(vals);
        return vals;
    }
}