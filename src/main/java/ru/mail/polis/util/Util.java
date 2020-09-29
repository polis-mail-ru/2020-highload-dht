package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Util {
    public static byte[] byteBufferToBytes(@NotNull final ByteBuffer buffer) {
        ByteBuffer bufCopy = buffer.duplicate();
        byte[] bytes = new byte[bufCopy.remaining()];
        bufCopy.get(bytes,0,bytes.length);
        return bytes;
    }
}
