package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
        iterator.seek(ByteBufferConverter.toArrayShifted(from));
        return new RockRecordIterator(iterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] result = db.get(ByteBufferConverter.toArrayShifted(key));
            if (result == null) {
                final String stringKey = StandardCharsets.UTF_8.decode(key).toString();
                throw new NoSuchElementLiteException("cant find element " + stringKey);
            }
            return ByteBuffer.wrap(result);
        } catch (RocksDBException exception) {
            throw new IOException("Getting error", exception);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(ByteBufferConverter.toArrayShifted(key), ByteBufferConverter.toArray(value));
        } catch (RocksDBException exception) {
            throw new IOException("Upserting error", exception);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(ByteBufferConverter.toArrayShifted(key));
        } catch (RocksDBException exception) {
            throw new IOException("Removing error", exception);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException exception) {
            throw new IOException("Compaction error", exception);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException exception) {
            throw new IOException("Closing error", exception);
        }
    }

    static DAO create(final File data) throws IOException {
        RocksDB.loadLibrary();
        try {
            final Options options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            final RocksDB db = RocksDB.open(options, data.getAbsolutePath());
            return new RocksDAO(db);
        } catch (RocksDBException exception) {
            throw new IOException("Creating error", exception);
        }
    }
}
