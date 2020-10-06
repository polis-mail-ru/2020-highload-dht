package ru.mail.polis.dao.bmendli;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table extends Closeable {

    void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value, final long expireTime);

    void remove(@NotNull final ByteBuffer key);

    @NotNull
    Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException;

    long size();
}
