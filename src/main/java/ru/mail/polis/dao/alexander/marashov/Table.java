package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table extends Closeable {

    @NotNull
    Iterator<Cell> iterator(@NotNull ByteBuffer from) throws IOException;

    void upsert(
            @NotNull ByteBuffer key,
            @NotNull ByteBuffer value,
            long expiresTimestamp
    ) throws IOException;

    void remove(@NotNull ByteBuffer key) throws IOException;

    long sizeInBytes();

    int size();

    File getFile();
}
