package ru.mail.polis.dao.IgorManikhin;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.io.File;
import java.util.NoSuchElementException;

public final class DAOImpl implements DAO {
    private final RocksDB db;
    final Options options;

    /**
     * @param data
     * @throws IOException
     */
    public DAOImpl(final File data) throws IOException {
        RocksDB.loadLibrary();
        try {
            options = new Options().setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            db = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException except) {
            throw new IOException("Create error", except);
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
                throw new NoSuchElementException("No such element " + stringKey);
            }

            return ByteBuffer.wrap(result);
        } catch (RocksDBException except) {
            throw new IOException("Get error", except);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull ByteBuffer value) throws IOException {
        try {
            db.put(ByteConvertor.toUnsignedArray(key), ByteConvertor.toArray(value));
        } catch (RocksDBException exception) {
            throw new IOException("Upsert error", exception);
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
