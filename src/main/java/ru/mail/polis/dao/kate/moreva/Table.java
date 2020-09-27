package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table {
    @NotNull
    Iterator<Cell> iterator(@NotNull ByteBuffer from) throws IOException;

    long sizeInBytes();

    void upsert(ByteBuffer key, ByteBuffer value);

    void remove(ByteBuffer key);

    void close() throws IOException;

}
