package ru.mail.polis.dao.Mariarheon;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import ru.mail.polis.Record;
import org.rocksdb.util.BytewiseComparator;
import ru.mail.polis.dao.DAO;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DAOImpl implements DAO {
    static {
        RocksDB.loadLibrary();
    }

    private final RocksDB db;

    public DAOImpl(final File path) throws DAOException {
        final Options options = new Options()
                .setCreateIfMissing(true)
                .setComparator(new BytewiseComparator(new ComparatorOptions()));
        try {
            db = RocksDB.open(options, path.getAbsolutePath());
        } catch (RocksDBException ex) {
            throw new DAOException("Error: can't create RocksDb instance", ex);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from){
        final var iter = db.newIterator() ;
        iter.seek(ByteBufferUtils.toArray(from));
        return new RocksIteratorAdapter(iter);
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws DAOException {
        try{
            db.put(ByteBufferUtils.toArray(key), ByteBufferUtils.toArray(value));
        } catch (RocksDBException ex) {
            throw new DAOException("Error: can't upsert record", ex);
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws DAOException {
        try{
            db.delete(ByteBufferUtils.toArray(key));
        } catch (RocksDBException ex) {
            throw new DAOException("Error: can't remove record", ex);
        }
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull ByteBuffer key) throws DAOException, NoSuchElementException {
        try{
            final var record = db.get(ByteBufferUtils.toArray(key));
            if (record == null){
                throw new NoSuchElementException("Error finding record with key" + key.toString());
            }
            return ByteBufferUtils.toByteBuffer(record);
        } catch (RocksDBException ex) {
            throw new DAOException("Error: can't ger record", ex);
        }
    }

    @Override
    public void compact() throws DAOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new DAOException("Error: can't compact", e);
        }
    }

    @Override
    public void close(){
        db.close();
    }

}
