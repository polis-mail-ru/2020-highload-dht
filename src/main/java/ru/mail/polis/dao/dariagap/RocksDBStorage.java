package ru.mail.polis.dao.dariagap;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
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

    public RocksDBStorage(@NotNull final File path) throws IOException {
        try {
            final Options options = new Options()
                    .setCreateIfMissing(true)
                    .setComparator(BuiltinComparator.BYTEWISE_COMPARATOR);
            db = RocksDB.open(options, path.getAbsolutePath());
        }
        catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    @NotNull
    public Iterator<Record> iterator(@NotNull ByteBuffer from) throws IOException{
        final RocksIterator iter = db.newIterator();
        iter.seek(Util.pack(from));
        return new RecordIterator(iter);
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {
        try {
            db.put(Util.pack(key),Util.byteBufferToBytes(value));
        }
        catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {
        try {
            db.delete(Util.pack(key));
        }
        catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        }
        catch (RocksDBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() {
        db.close();
    }
}
