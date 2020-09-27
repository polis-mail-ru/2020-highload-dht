package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table {
    @NotNull
    Iterator<Cell> iterator(@NotNull ByteBuffer from);

    void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value);

    void remove(@NotNull ByteBuffer key) throws IOException;

    long sizeInBytes();

    void close();
}
