package ru.mail.polis.dao.stasyanoi;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomServer;
import ru.mail.polis.service.stasyanoi.IteratorImpl;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DAOImpl implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB storageInstance;
    private IteratorImpl recordIterator;

    /**
     * Creates a dao implementation based on the given dir.
     *
     * @param data - db storage location
     */
    public DAOImpl(final File data) {
        Options options = new Options().setCreateIfMissing(true);
        final ComparatorOptions comparatorOptions = new ComparatorOptions();
        options = options.setComparator(new ComparatorImpl(comparatorOptions));
        try {
            storageInstance = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(final @NotNull ByteBuffer from) {
        return getIterator(from);
    }

    private Iterator<Record> getIterator(final @NotNull ByteBuffer from) {
        if (recordIterator != null) {
            recordIterator.close();
        }
        recordIterator = new IteratorImpl(from, storageInstance.newIterator());
        return recordIterator;
    }

    @Override
    public void upsert(final @NotNull ByteBuffer key, final @NotNull ByteBuffer value) {
        try {
            storageInstance.put(CustomServer.toBytes(key), CustomServer.toBytes(value));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public ByteBuffer get(final @NotNull ByteBuffer key) throws NoSuchElementException {
        byte[] body;
        try {
            body = storageInstance.get(CustomServer.toBytes(key));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
        if (body == null) {
            throw new NoSuchElementException("No such key " + key.toString());
        }
        return CustomServer.fromBytes(body);
    }

    @Override
    public void remove(final @NotNull ByteBuffer key) {
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

    @Override
    public void compact() {
        try {
            storageInstance.compactRange();
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
