package ru.mail.polis.dao.manikhin;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class DAOImpl implements DAO {
    private final RocksDB db;
    final Options options;

    /**
     * DAO implementation over RocksDB.
     *
     * @param data input data for init DAO
     * @throws IOException DAO init exception
     */
    public DAOImpl(final File data) throws IOException {
        RocksDB.loadLibrary();
        try {
            options = new Options().setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            db = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException except) {
            throw new IOException("Init error", except);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator iterator = db.newIterator();
        iterator.seek(ByteConvertor.toUnsignedArray(from));
        return new RecordIterator(iterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] result = db.get(ByteConvertor.toUnsignedArray(key));

            if (result == null) {
                final String stringKey = StandardCharsets.UTF_8.decode(key).toString();
                throw new NoSuchElementException(String.format("No such element: %s", stringKey));
            }

            return ByteBuffer.wrap(result);
        } catch (RocksDBException except) {
            throw new IOException("Get error", except);
        }
    }

    @Override
    public TimestampRecord getTimestampRecord(@NotNull final ByteBuffer key)
            throws NoSuchElementException, IOException {
        try {
            final byte[] packedKey = ByteConvertor.toUnsignedArray(key);
            final byte[] valueByteArray = db.get(packedKey);
            return TimestampRecord.fromBytes(valueByteArray);
        } catch (RocksDBException e) {
            throw new IOException("getting error", e);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(ByteConvertor.toUnsignedArray(key), ByteConvertor.toArray(value));
        } catch (RocksDBException exception) {
            throw new IOException("Upsert error", exception);
        }
    }

    @Override
    public void upsertTimestampRecord(@NotNull final ByteBuffer keys,
                                      @NotNull final ByteBuffer values) throws IOException {
        try {
            final TimestampRecord record = TimestampRecord.fromValue(values, System.currentTimeMillis());
            final byte[] packedKey = ByteConvertor.toUnsignedArray(keys);
            final byte[] arrayValue = record.toBytes();
            db.put(packedKey, arrayValue);
        } catch (RocksDBException e) {
            throw new IOException("upserting error", e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(ByteConvertor.toUnsignedArray(key));
        } catch (RocksDBException except) {
            throw new IOException("Remove error", except);
        }
    }

    @Override
    public void removeTimestampRecord(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] packedKey = ByteConvertor.toUnsignedArray(key);
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
        } catch (RocksDBException except) {
            throw new IOException("Compact error", except);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException except) {
            throw new IOException("Close error", except);
        }
    }
}
