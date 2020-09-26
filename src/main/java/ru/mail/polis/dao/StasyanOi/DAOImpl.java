package ru.mail.polis.dao.StasyanOi;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.StasyanOi.CustomServer;
import ru.mail.polis.service.StasyanOi.IteratorImpl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class DAOImpl implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB storageInstance;

    public DAOImpl(File data) {
        Options options = new Options().setCreateIfMissing(true);
        try {
            storageInstance = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) throws IOException {
        return new IteratorImpl(storageInstance.newIterator());
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {
        try {
            storageInstance.put(CustomServer.toBytes(key), CustomServer.toBytes(value));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {
        try {
            storageInstance.delete(CustomServer.toBytes(key));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        storageInstance.close();
    }
}
