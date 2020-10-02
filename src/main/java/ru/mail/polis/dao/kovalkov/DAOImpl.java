package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.CompressionType;
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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
    public ByteBuffer get(@NotNull final ByteBuffer key) throws NoSuchElementException {
        try {
            final byte[] valueByteArray = db.get(BufferConverter.convertBytes(key));
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
            db.put(BufferConverter.convertBytes(key), unfoldToBytes(value));
        } catch (RocksDBException e) {
            log.error("Rocks upsert error: ", e);
            throw new RuntimeException("Rocks upsert error: ", e);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        try {
            db.delete(BufferConverter.convertBytes(key));
        } catch (RocksDBException e) {
            log.error("Remove error: ", e);
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

    /**
     * Create DAO based on RocksDB.
     *
     * @param data - store data.
     * @return - new DAO.
     */
    public static DAO createDAO(final File data) {
        try {
            final var options = new Options()
                    .setCreateIfMissing(true)
                    .setCompressionType(CompressionType.NO_COMPRESSION)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            final RocksDB db = RocksDB.open(options, data.getAbsolutePath());
            return new DAOImpl(db);
        } catch (RocksDBException e) {
            throw new RuntimeException("RocksDB instantiation failed!", e);
        }
    }
}
