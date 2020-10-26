package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.Record;
import ru.mail.polis.service.codearound.Value;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Storage interface.
 *
 * @author Vadim Tsesko
 * @author Dmitry Schitinin
 */
public interface DAO extends Closeable {

    /**
     * Provides iterator (possibly empty) over {@link Record}s starting at "from" key (inclusive)
     * in <b>ascending</b> order according to {@link Record#compareTo(Record)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    @NotNull
    Iterator<Record> iterator(@NotNull ByteBuffer from) throws IOException;

    /**
     * Provides iterator (possibly empty) over {@link Record}s starting at "from" key (inclusive)
     * until given "to" key (exclusive) in <b>ascending</b> order according to {@link Record#compareTo(Record)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    @NotNull
    default Iterator<Record> range(
            @NotNull ByteBuffer from,
            @Nullable ByteBuffer to) throws IOException {
        if (to == null) {
            return iterator(from);
        }

        if (from.compareTo(to) > 0) {
            return Iters.empty();
        }

        final Record bound = Record.of(to, ByteBuffer.allocate(0));
        return Iters.until(iterator(from), bound);
    }

    /**
     * Obtains {@link Record} corresponding to given key.
     *
     * @param key - key searched to read some value
     * @throws NoSuchElementException if no such record
     */
    @NotNull
    default ByteBuffer get(@NotNull ByteBuffer key) throws IOException, NoSuchElementException {
        final Iterator<Record> iter = iterator(key);
        if (!iter.hasNext()) {
            throw new NoSuchElementException("Not found");
        }

        final Record next = iter.next();
        if (next.getKey().equals(key)) {
            return next.getValue();
        } else {
            throw new NoSuchElementException("Not found");
        }
    }

    /**
     * Inserts or updates value by given key.
     *
     * @param key - key either to add a record or to modify existing one
     * @param value key-bound value
     */
    void upsert(
            @NotNull ByteBuffer key,
            @NotNull ByteBuffer value) throws IOException;

    /**
     * Removes value by given key.
     *
     * @param key - key searched to remove specific record
     */
    void remove(@NotNull ByteBuffer key) throws IOException;

    /**
     * resolves timestamp-featured reading data by key specified.
     *
     * @param key - key searched to read some value
     */
    Value getValue(@NotNull ByteBuffer key) throws IOException, NoSuchElementException;

    /**
     * commits timestamp-featured record push or modification by key specified.
     *
     * @param key - key either to add a record or to modify existing one
     * @param value - key-bound value
     */
    void upsertValue(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException;

    /**
     * commits timestamp-featured record deletion by key specified.
     *
     * @param key - key searched to remove specific record
     */
    void removeValue(@NotNull ByteBuffer key) throws IOException;

    /**
     * Performs compaction.
     */
    default void compact() throws IOException {
        // Implement me when you get to stage 3
    }
}
