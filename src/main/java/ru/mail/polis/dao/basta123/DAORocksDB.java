package ru.mail.polis.dao.basta123;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;

public class DAORocksDB implements DAO {

    private final RocksDB rocksDBInstance;

    /**
     * database initialization.
     *
     * @param path DB location path
     */
    public DAORocksDB(final File path) {
        RocksDB.loadLibrary();
        final ComparatorOptions comOptions = new ComparatorOptions();
        final Options options = new Options()
                .setCreateIfMissing(true)
                .setComparator(new SingedBytesComparator(comOptions));
        try {
            rocksDBInstance = RocksDB.open(options, path.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException("rocksDBInstance can't open : ", e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(final @NotNull ByteBuffer from) {
        final RocksIterator rocksIterator = rocksDBInstance.newIterator();
        rocksIterator.seek(getByteArrayFromByteBuffer(from));
        return new MyRecordIter(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws NoSuchElementException, IOException {
        try {
            final byte[] valueByte = rocksDBInstance.get(getByteArrayFromByteBuffer(key));
            if (valueByte == null) {
                throw new NoSuchElementException("Not such value by this key");
            }
            return ByteBuffer.wrap(valueByte);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void upsert(final @NotNull ByteBuffer key, final @NotNull ByteBuffer value) throws IOException {
        try {
            final byte[] keyByte = getByteArrayFromByteBuffer(key);
            final byte[] valueByte = getByteArrayFromByteBuffer(value);
            rocksDBInstance.put(keyByte, valueByte);
        } catch (RocksDBException e) {
            throw new IOException("upsert ex: ", e);
        }
    }

    @Override
    public void remove(final @NotNull ByteBuffer key) throws IOException {
        try {
            rocksDBInstance.delete(getByteArrayFromByteBuffer(key));
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        try {
            rocksDBInstance.syncWal();
            rocksDBInstance.closeE();
        } catch (RocksDBException e) {
            throw new RuntimeException("Error on closing:", e);
        }
    }

    @Override
    public void compact() {
        try {
            rocksDBInstance.compactRange();
        } catch (RocksDBException e) {
            throw new RuntimeException("Compact error :", e);
        }
    }
}
