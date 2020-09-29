package ru.mail.polis.dao.kovalkov.Utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferConverter {

    /** Dont touch these methods
     *  It all tests doesn't work for anyone without them.
     *
     * Environment:
     * RE version: OpenJDK Runtime Environment AdoptOpenJDK (11.0.8+10) (build 11.0.8+10)
     * Java VM: OpenJDK 64-Bit Server VM AdoptOpenJDK (11.0.8+10, mixed mode, tiered,
     * compressed oops, g1 gc, windows-amd64)
     * Host: AMD Ryzen 7 4800HS with Radeon Graphics
     * 16 cores, 15G,  Windows 10 , 64 bit Build 19041 (10.0.19041.292)
     * Firewall: turned-off
     *
     * Use: ByteBuffer.wrap(copy); instead {@link #foldToBuffer(byte[])} {@link #convertBuffer(ByteBuffer)}
     * Get: Unexpected exception thrown.
     * org.gradle.internal.remote.internal.MessageIOException: Could not read message from '/127.0.0.1:62529'.
     */

    public static byte[] convertBuffer(@NotNull final ByteBuffer key) {
        final byte[] bytes = unfoldToBytes(key);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] -= Byte.MIN_VALUE;
        }
        return bytes;
    }

    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
        final byte[] bytes = new byte[b.limit()];
        b.get(bytes);
        b.clear();
        return bytes;
    }


    public static ByteBuffer foldToBuffer(@NotNull final byte[] key) {
        final byte[] copy = Arrays.copyOf(key, key.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(copy);
    }
}
