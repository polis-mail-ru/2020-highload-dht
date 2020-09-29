package ru.mail.polis.dao.kovalkov.Utils;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class BufferConverter {
    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
        final byte[] bytes = new byte[b.limit()];
        b.get(bytes);
        b.clear();
        return bytes;
    }
}
