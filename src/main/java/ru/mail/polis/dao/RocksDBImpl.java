package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RocksDBImpl implements DAO{
    static {
        RocksDB.loadLibrary();
    }

    private static final Logger log = LoggerFactory.getLogger(RocksDBImpl.class);
    private RocksDB db;

    public RocksDBImpl(final File data) {
        final Options options = new Options().setCreateIfMissing(true)
                .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
        try{
            db = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            log.error("Rocks open error: ", e);
            throw new RuntimeException("Rocks open error: ", e);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        rocksIterator.seek(unfoldToBytes(from));
        return new RecordIterator(rocksIterator);
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull ByteBuffer key) {
        try {
            final byte[] value = db.get(unfoldToBytes(key));
            if (value == null) {
                log.error("Get method can't find value by key {} ",unfoldToBytes(key));
                throw new NoSuchElementException();
            }
            return ByteBuffer.wrap(value);
        } catch (RocksDBException e) {
            log.error("Rocks get error: ", e);
            throw new RuntimeException("Rocks open error: ", e);
        }
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) {
            try {
                db.put(unfoldToBytes(key), unfoldToBytes(value));
            } catch (RocksDBException e) {
                log.error("Rocks upsert error: ", e);
                throw new RuntimeException("Rocks upsert error: ", e);
            }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) {
        try {
            db.delete(unfoldToBytes(key));
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
        try {
            db.syncWal();
            db.closeE();
        } catch (RocksDBException e) {
            log.error("Close error: ",e);
            throw new RuntimeException("Close error: ", e);
        }
    }

    public static byte[] unfoldToBytes(@NotNull final ByteBuffer b) {
        final byte[] bytes = new byte[b.limit()];
        b.get(bytes);
        b.clear();
        return bytes;
    }
}