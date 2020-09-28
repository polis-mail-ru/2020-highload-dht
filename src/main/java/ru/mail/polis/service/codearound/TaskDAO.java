package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.file.Files;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TaskDAO implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    File dbLocalDir;
    private RocksDB db;

    public TaskDAO(final RocksDB db) {
        this.db = db;
    }

    /**
     * class instance const
     *
     * @param data file object that stores key-value records
     */
    public TaskDAO(@NotNull final File data) {
        final Options opts = new Options();
        opts.setCreateIfMissing(true);
        dbLocalDir = data;
        try {
            Files.createDirectories(dbLocalDir.getParentFile().toPath());
            Files.createDirectories(dbLocalDir.getAbsoluteFile().toPath());
            db = RocksDB.open(opts, dbLocalDir.getAbsolutePath());
        } catch (IOException | RocksDBException exc) {
            System.out.println("Error initializing DB instance in local file system - DB connection not available\n");
        }
        System.out.println("DB initializing finished - storage function enabled\n");
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(from);
        rocksIterator.seek(byteArray);
        return new RocksRecordIterator(rocksIterator);
    }

    /**
     * insert/update dual method definition
     * @param key either new or modified record key
     * @param value key-bound value
     */
    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] keys = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] vals = DAOByteOnlyConverter.readByteArray(value);
            db.put(keys, vals);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * remove method definition
     * @param key - key which related record to be removed
     */
    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(key);
            db.delete(byteArray);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * get method definition
     * @param key - key to occur a match record
     */
    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] keys = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] vals = db.get(keys);
            if (vals == null) {
                throw new NoSuchElementException("No record found by key " + key.toString());
            }
            return ByteBuffer.wrap(vals);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * stop handling request and drop active connection
     */
    @Override
    public void close() {
        db.close();
    }
}
