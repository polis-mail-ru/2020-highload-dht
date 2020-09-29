package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
import org.rocksdb.util.BytewiseComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

import java.io.File;
import java.util.Iterator;

public class RocksDBImpl {
    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB db;
    protected static final Logger log = LoggerFactory.getLogger(RocksDBImpl.class);

    RocksDBImpl(final File data) {
        try {
//            final BytewiseComparator comparator = new BytewiseComparator(new ComparatorOptions());
            final Options options = new Options().setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            db = RocksDB.open(options, data.getAbsolutePath());
        } catch (RocksDBException e) {
            log.error("Rocks open error: ", e);
            throw new RuntimeException("Rocks open error: ", e);
        }
    }

    @NotNull
    public Iterator<Record> iterator(@NotNull byte[] from) {
        final RocksIterator rocksIterator = db.newIterator();
        rocksIterator.seek(from);
        return new RecordIterator(rocksIterator);
    }

    public void put(@NotNull byte[] key, @NotNull byte[] value) {
        try {
            db.put(key, value);
        } catch (RocksDBException e) {
            log.error("Rocks upsert error: ", e);
            throw new RuntimeException("Rocks upsert error: ", e);
        }
    }

    public void delete(@NotNull byte[] key) {
        try {
            db.delete(key);
        } catch (RocksDBException e) {
            log.error("Remove error: ",e);
            throw new RuntimeException("Remove error: ", e);
        }
    }

    public void compact() {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            log.error("Compact error: ",e);
            throw new RuntimeException("Compact error: ", e);
        }
    }

    public void close() {
        db.close();
    }
}