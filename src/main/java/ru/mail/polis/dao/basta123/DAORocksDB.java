package ru.mail.polis.dao.basta123;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.basta123.TimestampValue;
import ru.mail.polis.service.basta123.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DAORocksDB implements DAO {

    private static final Logger log = LoggerFactory.getLogger(DAORocksDB.class);

    private RocksDB rocksDBInstance;

    /**
     * class instance const.
     *
     * @param data - file to store key-value records
     */
    public DAORocksDB(@NotNull final File data) {
        RocksDB.loadLibrary();
        final Options options = new Options().setCreateIfMissing(true);
        try {
            rocksDBInstance = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException("rocksDBInstance can't open : ", e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = rocksDBInstance.newIterator();
        rocksIterator.seek(Utils.bufToArray(from));
        return new RecordIter2(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] keys = Utils.bufToArray(key);
            final byte[] vals = rocksDBInstance.get(keys);
            if (vals == null) {
                throw new NoSuchElementException("value doesnt found: " + key.toString());
            }
            return ByteBuffer.wrap(vals);
        } catch (RocksDBException exc) {
            throw new IOException("get exception:", exc);
        }
    }

    /**
     * executes insertion/update on record specified.
     *
     * @param key   - key that should match for attaching a value to server response
     * @param value - key-bound value
     */
    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] keys = Utils.bufToArray(key);
            final byte[] vals = Utils.readArrayBytes(value);
            rocksDBInstance.put(keys, vals);
        } catch (RocksDBException e) {
            throw new IOException("upsert ex: ",e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteArray = Utils.bufToArray(key);
            rocksDBInstance.delete(byteArray);
        } catch (RocksDBException e) {
            throw new IOException("remove exception:", e);
        }
    }

    /**
     * resolves timestamp-featured reading data by key specified.
     *
     * @param key - key searched to read some value
     */
    @Override
    public TimestampValue getTimestampValue(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] value = getValueFromBytes(key);
            return TimestampValue.getTimestampValueFromBytes(value);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * commits timestamp-featured record push or modification by key specified.
     *
     * @param key   - key either to add a record or to modify existing one
     * @param value - key-bound value
     */
    @Override
    public void upsertTimestampValue(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            rocksDBInstance.put(Utils.bufToArray(key),
                    TimestampValue.getBytesFromTimestampValue(false,
                            System.currentTimeMillis(),
                            value));
        } catch (RocksDBException e) {
            log.error("error when putting a value in DB");
            throw new IOException(e);
        }
    }

    /**
     * commits timestamp-featured record deletion by key specified.
     *
     * @param key - key searched to remove specific record
     */
    @Override
    public void removeTimestampValue(@NotNull final ByteBuffer key) throws IOException {
        try {
            rocksDBInstance.put(Utils.bufToArray(key),
                    TimestampValue.getBytesFromTimestampValue(true,
                            System.currentTimeMillis(),
                            ByteBuffer.allocate(0)));
        } catch (RocksDBException e) {
            log.error("error when deleting a value from DB");
            throw new IOException(e);
        }
    }

    /**
     * implements processing for a value derived from ByteBuffer.
     *
     * @param key - key searched to remove specific record
     * @return value readable from byte array
     */
    private byte[] getValueFromBytes(@NotNull final ByteBuffer key) throws RocksDBException {
        final byte[] array = Utils.bufToArray(key);
        final byte[] value = rocksDBInstance.get(array);
        if (value == null) {
            throw new NoSuchElementException("key doesn't found");
        }
        return value;
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
