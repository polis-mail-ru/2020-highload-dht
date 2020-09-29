package ru.mail.polis.dao.dmrnz;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

interface Table {
    @NotNull
    Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException;

    @NotNull
    Iterator<Cell> reverseIterator(@NotNull final ByteBuffer from) throws IOException;

    @NotNull
    Iterator<Cell> reverseIterator() throws IOException;

    void upsert(@NotNull final ByteBuffer key,
                @NotNull final ByteBuffer value) throws IOException;

    void remove(@NotNull final ByteBuffer key) throws IOException;

    long sizeInBytes() throws IOException;
}
