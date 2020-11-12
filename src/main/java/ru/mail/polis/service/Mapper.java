package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Mapper {

    private static final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    private Mapper() {

    }

    /**
     * Convert long to byte array.
     *
     * @param number - long to convert.
     * @return - byte array.
     */
    public static byte[] longToBytes(final long number) {
        buffer.putLong(0, number);
        return buffer.array();
    }

    /**
     * Convert byte array to long.
     *
     * @param bytes - bytes to convert.
     * @return - parsed long.
     */
    public static long bytesToLong(final byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }

    /**
     * ByteBuffer to byte array.
     *
     * @param buffer - input buffer.
     * @return byte array.
     */
    @NotNull
    public static byte[] toBytes(final ByteBuffer buffer) {
        final int oldPosition = buffer.position();
        final byte[] bytes = new byte[buffer.limit()];
        buffer.rewind();
        buffer.get(bytes);
        buffer.position(oldPosition);
        return bytes;
    }

    /**
     * Create a byte buffer from byte array.
     *
     * @param bytes - input byte array.
     * @return - created byte buffer.
     */
    public static ByteBuffer fromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
}
