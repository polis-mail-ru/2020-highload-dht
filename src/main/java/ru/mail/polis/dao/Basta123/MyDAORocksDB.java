package ru.mail.polis.dao.Basta123;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Basta123.MyHTTPServer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;

import static ru.mail.polis.service.Basta123.Utils.getByteArrayFromByteBuffer;

public class MyDAORocksDB implements DAO {


    private RocksDB rocksDBInstance;
    private MyRecordIter recordIter;


    public MyDAORocksDB(final File path) {

        RocksDB.loadLibrary();
        ComparatorOptions comOptions = new ComparatorOptions();
        Options options = new Options().setCreateIfMissing(true);
        options.setComparator(new MyComparator(comOptions));
        try {
            rocksDBInstance = RocksDB.open(options, path.getPath());
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
    public void upsert(final @NotNull ByteBuffer key, final @NotNull ByteBuffer value) {
        try {
            byte[] keyByte = getByteArrayFromByteBuffer(key);
            byte[] valueByte = getByteArrayFromByteBuffer(value);
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

