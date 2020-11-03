package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
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
        final RocksIterator iterator = db.newIterator();
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
                throw new NoSuchElementLiteException("Can't find element " + stringKey);
            }
            return ByteBuffer.wrap(result);
        } catch (RocksDBException e) {
            throw new IOException("getting error", e);
        }
    }

    /**
     * Get record from DB.
     *
     * @param key - key
     * @return record
     */
    @NotNull
    public TimestampRecord getRecordWithTimestamp(@NotNull final ByteBuffer key)
            throws NoSuchElementLiteException, IOException {
        try {
            final byte[] packedKey = ByteBufferConverter.toArrayShifted(key);
            final byte[] valueByteArray = db.get(packedKey);
            return TimestampRecord.fromBytes(valueByteArray);
        } catch (RocksDBException e) {
            throw new IOException("getting error", e);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(ByteBufferConverter.toArrayShifted(key), ByteBufferConverter.toArray(value));
        } catch (RocksDBException e) {
            throw new IOException("upserting error", e);
        }
    }

    /**
     * Put record into DB.
     *
     * @param keys   to define key
     * @param values to define value
     */
    public void upsertRecordWithTimestamp(@NotNull final ByteBuffer keys,
                                          @NotNull final ByteBuffer values) throws IOException {
        try {
            final TimestampRecord record = TimestampRecord.fromValue(values, System.currentTimeMillis());
            final byte[] packedKey = ByteBufferConverter.toArrayShifted(keys);
            final byte[] arrayValue = record.toBytes();
            db.put(packedKey, arrayValue);
        } catch (RocksDBException e) {
            throw new IOException("upserting error", e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(ByteBufferConverter.toArrayShifted(key));
        } catch (RocksDBException e) {
            throw new IOException("removing error", e);
        }
    }

    /**
     * Delete record from DB.
     *
     * @param key to define key
     */
    public void removeRecordWithTimestamp(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] packedKey = ByteBufferConverter.toArrayShifted(key);
            final TimestampRecord record = TimestampRecord.tombstone(System.currentTimeMillis());
            final byte[] arrayValue = record.toBytes();
            db.put(packedKey, arrayValue);
        } catch (RocksDBException e) {
            throw new IOException("removing error",e);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new IOException("compaction error", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException e) {
            throw new IOException("closing error", e);
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
        } catch (RocksDBException e) {
            throw new IOException("creating error", e);
        }
    }
}
