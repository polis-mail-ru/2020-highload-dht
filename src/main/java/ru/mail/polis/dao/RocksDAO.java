package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.BytewiseComparator;
import ru.mail.polis.Record;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public final class RocksDAO implements DAO {

    private final RocksDB db;

    private RocksDAO(final RocksDB db) {
        this.db = db;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {

        final var iterator = db.newIterator();
        iterator.seek(toArray(from));
        return new RockRecordIterator(iterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final var result = db.get(toArray(key));
            if (result == null) {
                throw new NoSuchElementLite("cant find element " + key.toString());
            }
            return ByteBuffer.wrap(result);
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(toArray(key), toArray(value));
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(toArray(key));
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    static DAO create(final File data) throws IOException {
        RocksDB.loadLibrary();
        try {
            final var comparator = new BytewiseComparator(new ComparatorOptions());
            final var options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(comparator);
            final var db = RocksDB.open(options, data.getAbsolutePath());
            return new RocksDAO(db);
        } catch (RocksDBException exception) {
            throw new IOException("", exception);
        }
    }

    /**
     * Convert ByteBuffer from java.nio to byte array.
     *
     * @param buffer byte buffer
     * @return array bytes
     */
    public static byte[] toArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }
}
