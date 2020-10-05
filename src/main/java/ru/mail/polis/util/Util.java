package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Util {
    public static byte[] byteBufferToBytes(@NotNull final ByteBuffer buffer) {
        ByteBuffer bufCopy = buffer.duplicate();
        byte[] bytes = new byte[bufCopy.remaining()];
        bufCopy.get(bytes,0,bytes.length);
        bufCopy.clear();
        return bytes;
    }

    public static byte[] pack(@NotNull final ByteBuffer buffer){
        byte[] bytes = byteBufferToBytes(buffer);
        for (int i = 0; i<bytes.length; i++){
            bytes[i] ^= Byte.MIN_VALUE;
        }
        return bytes;
    }

    public static ByteBuffer unpack(@NotNull final byte[] bytes){
        byte[] bytesCopy = bytes.clone();
        for (int i = 0; i<bytesCopy.length; i++){
            bytesCopy[i] ^= Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(bytesCopy);
    }
}
