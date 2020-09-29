package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DAOImpl implements DAO {
    private final RocksDB db;
    protected static final Logger log = LoggerFactory.getLogger(DAOImpl.class);

    private DAOImpl(final RocksDB db) {
        this.db = db;
    }

    public static class RecordIterator implements Iterator<Record>, AutoCloseable {

        private final RocksIterator rocksIterator;

        RecordIterator(@NotNull final RocksIterator iterator) {
            this.rocksIterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return rocksIterator.isValid();
        }

        @Override
        public Record next() throws IllegalStateException {
            if (!hasNext()) {
                throw new IllegalStateException("No further");
            }
            final byte[] bytesKey = rocksIterator.key();
            final ByteBuffer bufferKey = foldToBuffer(bytesKey);
            final byte[] bytesValue = rocksIterator.value();
            final ByteBuffer bufferValue = ByteBuffer.wrap(bytesValue);
            final var record = Record.of(bufferKey, bufferValue);
            rocksIterator.next();
            return record;
        }

        @Override
        public void close() {
            rocksIterator.close();
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        rocksIterator.seek(converBytes(from));
        return new RecordIterator(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws NoSuchElementException {
        try {
            final byte[] valueByteArray = db.get(converBytes(key));
            if (valueByteArray == null) {
                log.error("No such value key {}.", key);
                throw new NoSuchElementException("Not such value by this key");
            }
            return ByteBuffer.wrap(valueByteArray);
        } catch (RocksDBException e) {
            log.error("Getting error: ",e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        try {
            db.put(converBytes(key), unfoldToBytes(value));
        } catch (RocksDBException e) {
            log.error("Rocks upsert error: ", e);
            throw new RuntimeException("Rocks upsert error: ", e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        try {
            db.delete(converBytes(key));
        } catch (RocksDBException e) {
            log.error("Remove error: ",e);
            throw new RuntimeException("Remove error: ", e);
        }
    }

    @Override
    public void compact() {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            log.error("Compact error: ",e);
            throw new RuntimeException("Compact error: ", e);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    private static byte[] unfoldToBytes(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] array = new byte[copy.remaining()];
        copy.get(array);
        return array;
    }

    private static byte[] converBytes(@NotNull final ByteBuffer key) {
        final byte[] arrayKey = unfoldToBytes(key);
        for (int i = 0; i < arrayKey.length; i++) {
            arrayKey[i] -= Byte.MIN_VALUE;
        }
        return arrayKey;
    }

    private static ByteBuffer foldToBuffer(@NotNull final byte[] key) {
        final byte[] copy = Arrays.copyOf(key, key.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(copy);
    }

    /**
     * Create DAO based on RocksDB.
     *
     * @param data - store data.
     * @return - new DAO.
     */
    public static DAO createDAO(final File data) {
        try {
            final var options = new Options();
            options.setCreateIfMissing(true);
            options.setCompressionType(CompressionType.NO_COMPRESSION);
            options.setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            options.setMaxBackgroundCompactions(2);
            options.setMaxBackgroundFlushes(2);
            final WriteOptions wOptions = new WriteOptions();
            wOptions.setDisableWAL(true);
            final RocksDB db = RocksDB.open(options, data.getAbsolutePath());
            return new DAOImpl(db);
        } catch (RocksDBException e) {
            throw new RuntimeException("RocksDB instantiation failed!", e);
        }
    }
}
