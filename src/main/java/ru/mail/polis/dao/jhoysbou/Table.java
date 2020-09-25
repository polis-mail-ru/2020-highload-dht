package ru.mail.polis.dao.jhoysbou;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table {

    /**
     * Provides iterator (possibly empty) over {@link Record}s starting at "from" key (inclusive)
     * in <b>ascending</b> order according to {@link Record#compareTo(Record)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    @NotNull
    Iterator<Cell> iterator(@NotNull ByteBuffer from) throws IOException;

    /**
     * Inserts or updates value by given key.
     */
    void upsert(
            @NotNull ByteBuffer key,
            @NotNull ByteBuffer value) throws IOException;

    /**
     * Removes value by given key.
     */
    void remove(@NotNull ByteBuffer key) throws IOException;

    int size();

    long sizeInBytes();

    void close() throws IOException;
}
