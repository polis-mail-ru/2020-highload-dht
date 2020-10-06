package ru.mail.polis.dao.basta123;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;

public class MyDAORocksDB implements DAO {

    private RocksDB rocksDBInstance;
    private MyRecordIter recordIter;

    /**
     * database initialization.
     *
     * @param path DB location path
     */
    public MyDAORocksDB(final File path) {
        RocksDB.loadLibrary();
        final ComparatorOptions comOptions = new ComparatorOptions();
        final Options options = new Options().setCreateIfMissing(true);
        options.setComparator(new MyComparator(comOptions));
        try {
            rocksDBInstance = RocksDB.open(options, path.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException("rocksDBInstance can't open : ", e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(final @NotNull ByteBuffer from) {
        if (recordIter != null) {
            recordIter.close();
        }
        recordIter = new MyRecordIter(from, rocksDBInstance.newIterator());
        return recordIter;
    }

    @Override
    @NotNull
    public synchronized ByteBuffer get(@NotNull ByteBuffer key) throws IOException, NoSuchElementException {
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

    @Override
    public synchronized void upsert(final @NotNull ByteBuffer key, final @NotNull ByteBuffer value) {
        try {
            final byte [] keyByte = getByteArrayFromByteBuffer(key);
            final byte [] valueByte = getByteArrayFromByteBuffer(value);
            rocksDBInstance.put(keyByte, valueByte);
        } catch (RocksDBException e) {
            throw new RuntimeException("upsert ex: ", e);
        }
    }

    @Override
    public void remove(final @NotNull ByteBuffer key) {
        try {
            rocksDBInstance.delete(getByteArrayFromByteBuffer(key));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
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
