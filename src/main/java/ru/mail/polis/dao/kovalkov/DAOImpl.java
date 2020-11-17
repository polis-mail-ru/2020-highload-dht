package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.isNull;

public final class DAOImpl implements DAO {
    private static final Logger log = LoggerFactory.getLogger(DAOImpl.class);
    private final RocksDB db;

    private DAOImpl(final RocksDB db) {
        this.db = db;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        rocksIterator.seek(BufferConverter.convertBytes(from));
        return new RecordIterator(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws NoSuchElementException, IOException {
        byte[] valueByteArray = null;
        try {
             valueByteArray = db.get(BufferConverter.convertBytes(key));
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Getting error: ",e);
        }
        checkValueExist(valueByteArray, key);
        return ByteBuffer.wrap(valueByteArray);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(BufferConverter.convertBytes(key), BufferConverter.unfoldToBytes(value));
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Rocks upsert error: ", e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(BufferConverter.convertBytes(key));
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Remove error: ", e);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Compact error: ", e);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    @NotNull
    @Override
    public TimestampDataWrapper getWithTimestamp(@NotNull final ByteBuffer key)
            throws IOException, NoSuchElementException {
        byte[] bytesValue = null;
        try {
            final byte[] byteKey = BufferConverter.convertBytes(key);
             bytesValue = db.get(byteKey);
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Remove error: ", e);
        }
        checkValueExist(bytesValue, key);
        return TimestampDataWrapper.wrapFromBytesAndGetOne(bytesValue);
    }

    private void checkValueExist(final byte[] bytesValue, @NotNull final ByteBuffer key) {
        if (isNull(bytesValue)) {
            log.info("No such value key {}.", key);
            throw new NoSuchElementException("Not such value by this key");
        }
    }

    @Override
    public void removeWithTimestamp(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteKey = BufferConverter.convertBytes(key);
            final byte[] bytesValue = TimestampDataWrapper.getDeletedOne(System.currentTimeMillis()).toBytesFromValue();
            db.put(byteKey, bytesValue);
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Remove error: ", e);
        }
    }

    @Override
    public void upsertWithTime(@NotNull final ByteBuffer key, @NotNull final ByteBuffer values) throws IOException {
        try {
            final byte[] byteKey = BufferConverter.convertBytes(key);
            final byte[] bytesValue = TimestampDataWrapper.getOne(values, System.currentTimeMillis()).toBytesFromValue();
            db.put(byteKey, bytesValue);
        } catch (final RocksDBException e) {
            rockDBExceptionHandler("Rocks upsert error: ", e);
        }
    }

    private static void rockDBExceptionHandler(@NotNull final String msg,
                                               @NotNull final Exception e) throws IOException {
        log.error(msg, e);
        throw new IOException(msg, e);
    }

    /**
     * Create DAO based on RocksDB.
     *
     * @param data - store data.
     * @return - new DAO.
     */
    @NotNull
    public static DAO createDAO(@NotNull final File data) throws IOException {
        try {
            final var options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR)
                    .setAllowConcurrentMemtableWrite(true);
            options.enableWriteThreadAdaptiveYield();
            final var db = RocksDB.open(options, data.getAbsolutePath());
            return new DAOImpl(db);
        } catch (final RocksDBException e) {
            throw new IOException("RocksDB instantiation failed!", e);
        }
    }
}
