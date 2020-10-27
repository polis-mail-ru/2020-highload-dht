package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.service.Value;
import ru.mail.polis.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaoEngine implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    File dbLocalDir;
    private RocksDB db;
    private static final Logger LOGGER = Logger.getLogger(DaoEngine.class.getName());

    public DaoEngine(final RocksDB db) {
            this.db = db;
        }

    public DaoEngine(@NotNull final File data) {
        final BuiltinComparator comparator = BuiltinComparator.BYTEWISE_COMPARATOR;
        final Options options = new Options()
                .setCreateIfMissing(true)
                .setComparator(comparator);

        dbLocalDir = data;
        try {
            Files.createDirectories(dbLocalDir.getParentFile().toPath());
            Files.createDirectories(dbLocalDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, dbLocalDir.getAbsolutePath());
        } catch (IOException | RocksDBException exc) {
            LOGGER.log(Level.SEVERE, "Storage initialization failed", exc);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        final byte[] byteArray = Util.toShiftedArray(from);
        rocksIterator.seek(byteArray);
        return new RecordIterator(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] keys = Util.toShiftedArray(key);
            final byte[] vals = db.get(keys);
            if (vals == null) {
                throw new NoSuchElementException("No record found by key " + key.toString());
            }
            return ByteBuffer.wrap(vals);
        } catch (RocksDBException exc) {
            throw new IOException(exc);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] keys = Util.toShiftedArray(key);
            final byte[] vals = Util.toByteArray(value);
            db.put(keys, vals);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteArray = Util.toShiftedArray(key);
            db.delete(byteArray);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Value getValue(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] value = getValueFromBytes(key);
            return Value.composeFromBytes(value);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void upsertValue(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] convertedKey = Util.toShiftedArray(key);
            final byte[] timestamp = Value.resolveExistingValue(value, System.currentTimeMillis()).getValueBytes();
            db.put(convertedKey, timestamp);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void removeValue(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] convertedKey = Util.toShiftedArray(key);
            final byte[] value = Value.resolveDeletedValue(System.currentTimeMillis()).getValueBytes();
            db.put(convertedKey, value);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    private byte[] getValueFromBytes(@NotNull final ByteBuffer key) throws RocksDBException {
        final byte[] array = Util.toShiftedArray(key);
        final byte[] value = db.get(array);
        if (value == null) {
            throw new NoSuchElementException("No match key found, failed request");
        }
        return value;
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }
}
