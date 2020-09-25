package ru.mail.polis.dao.StasyanOi;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

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
        return null;
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {
storageInstance.close();
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
