package ru.mail.polis.dao.igor_manikhin;
import ru.mail.polis.Record;

import org.jetbrains.annotations.NotNull;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.RocksDBException;
import org.rocksdb.Options;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.util.BytewiseComparator;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class RocksImplDAO implements DAO {

    private final RocksDB db;

    public RocksImplDAO(@NotNull final File data) throws IOException {
        final BytewiseComparator comparator;
        final Options options;

        try {
            RocksDB.loadLibrary();
            comparator = new BytewiseComparator(new ComparatorOptions());
            options = new Options().setCreateIfMissing(true).setComparator(comparator);
            db = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException except) {
            throw new IOException("", except);
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
                throw new NoSuchElementException("cant find element " + key.toString());
            }

            return ByteBuffer.wrap(result);
        } catch (RocksDBException exception) {
            throw new IOException("Getting error", exception);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(ByteConvertor.toUnsignedArray(key), ByteConvertor.toArray(value));
        } catch (RocksDBException except){
            throw new IOException("", except);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException except) {
            throw new IOException("", except);
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {
        try {
            db.delete(ByteConvertor.toUnsignedArray(key));
        } catch (RocksDBException except) {
            throw new IOException("", except);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException except) {
            throw new IOException("", except);
        }
    }

}
