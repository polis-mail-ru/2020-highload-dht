package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
import org.rocksdb.util.BytewiseComparator;
import ru.mail.polis.Record;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static ru.mail.polis.util.Util.toByteArray;

public final class DAOImpl implements DAO {

    private final RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    private DAOImpl(final RocksDB db) {
        this.db = db;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator iterator = db.newIterator();
        iterator.seek(toByteArray(from));
        return new RecordIterator(iterator);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(toByteArray(key), toByteArray(value));
        } catch (RocksDBException exception) {
            throw new IOException(exception);
        }
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final var result = db.get(toByteArray(key));
            if (result == null) {
                throw new NoSuchElementException(String.format("No record found by key %s", key.toString()));
            }
            return ByteBuffer.wrap(result);
        } catch (RocksDBException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(toByteArray(key));
        } catch (RocksDBException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    static DAO init(final File data) throws IOException {
        RocksDB.loadLibrary();
        final var comparator = new BytewiseComparator(new ComparatorOptions());
        final var options = new Options()
            .setCreateIfMissing(true)
            .setComparator(comparator);
        try {
            final var db = RocksDB.open(options, data.getAbsolutePath());
            return new DAOImpl(db);
        } catch (RocksDBException exception) {
            throw new IOException(exception);
        }
    }
}