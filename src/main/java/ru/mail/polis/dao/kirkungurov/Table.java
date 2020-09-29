package ru.mail.polis.dao.kirkungurov;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table {

    @NotNull
    Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException;

    void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException;

    void remove(@NotNull final ByteBuffer key) throws IOException;

    void close() throws IOException;

    long size();
}
