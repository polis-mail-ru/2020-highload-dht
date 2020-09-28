package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.BytewiseComparator;
import ru.mail.polis.Record;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class RocksDBImpl implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;
    /**
     * Implemrnt DAO based on the given dir.
     *
     * @param path - db storage location
     */

    public RocksDBImpl(final File path) {
        try {
            final var comparator = new BytewiseComparator(new ComparatorOptions());
            final var options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(comparator);
            db = RocksDB.open(options, path.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final var iterator = db.newIterator();
        iterator.seek(fromByteBufferToByte(from));
        return new RecordIterator(iterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final var res = db.get(fromByteBufferToByte(key));
            if (res == null) {
            throw new NoSuchElementException();
        }
        return ByteBuffer.wrap(res);
    } catch (RocksDBException e) {
        throw new NoSuchElementException();
    }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
       try {
           db.put(fromByteBufferToByte(key), fromByteBufferToByte(value));
       } catch (RocksDBException e) {
           throw new RuntimeException(e);
       }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(fromByteBufferToByte(key));
        } catch (RocksDBException e) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void close() throws IOException {
        db.close();
    }

    /** Convert from ByteBuffer to Byte massive.
     *
     * @param buffer - ByteBuffer variable to convert
     */
    public static byte[] fromByteBufferToByte(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }
}
