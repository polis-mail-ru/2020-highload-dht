package ru.mail.polis.dao.dariagap;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.BuiltinComparator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class RocksDBStorage implements DAO {

    private final RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    /**
     * Open database.
     *
     * @param path to database
     */
    public RocksDBStorage(@NotNull final File path) throws IOException {
        try {
            final Options options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            db = RocksDB.open(options, path.getAbsolutePath());
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    @NotNull
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final RocksIterator iter = db.newIterator();
        iter.seek(Util.pack(from));
        return new RecordIterator(iter);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            db.put(Util.pack(key),Util.byteBufferToBytes(value));
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            db.delete(Util.pack(key));
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() {
        db.close();
    }
}
