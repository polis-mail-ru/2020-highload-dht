package ru.mail.polis.dao.StasyanOi;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.StasyanOi.CustomServer;
import ru.mail.polis.service.StasyanOi.IteratorImpl;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class DAOImpl implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB storageInstance;

    public DAOImpl(File data) {
        Options options = new Options().setCreateIfMissing(true);
        ComparatorOptions comparatorOptions = new ComparatorOptions();
        options = options.setComparator(new ComparatorImpl(comparatorOptions));
        try {
            storageInstance = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) {
        return new IteratorImpl(from, storageInstance.newIterator());
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) {
        try {
            storageInstance.put(CustomServer.toBytes(key), CustomServer.toBytes(value));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) {
        try {
            storageInstance.delete(CustomServer.toBytes(key));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        storageInstance.close();
    }
}
