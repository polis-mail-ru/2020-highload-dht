package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskDAO implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    File dbLocalDir;
    private RocksDB db;
    private static final Logger LOGGER = Logger.getLogger(TaskDAO.class.getName());

    public TaskDAO(final RocksDB db) {
            this.db = db;
        }

    /**
    * class instance const.
    * @param data - file to store key-value records
    */
    public TaskDAO(@NotNull final File data) {
        final Options opts = new Options();
        opts.setCreateIfMissing(true); // create db instance if one does not exist
        opts.setParanoidChecks(false); // drops strict data quality control while searching for corrupt items
        opts.setSkipStatsUpdateOnDbOpen(true); // abandons statistics updates every time db is opening to run
        opts.setAllowConcurrentMemtableWrite(true); // permits multithread memtable writes
        opts.enableWriteThreadAdaptiveYield(); // forces write batch to execute till mutex holding timeout

        opts.disableAutoCompactions(); // prevents from auto compactions as these are enabled by default
        opts.setCompactionStyle(CompactionStyle.UNIVERSAL) // applies universal (tiered) compaction algorithm
                .setCompressionType(CompressionType.LZ4_COMPRESSION); // replaces compression algorithm by default

        dbLocalDir = data;
        try {
            Files.createDirectories(dbLocalDir.getParentFile().toPath());
            Files.createDirectories(dbLocalDir.getAbsoluteFile().toPath());
            db = RocksDB.open(opts, dbLocalDir.getAbsolutePath());
        } catch (IOException | RocksDBException exc) {
            LOGGER.log(Level.SEVERE, "Storage initialization failed", exc);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(from);
        rocksIterator.seek(byteArray);
        return new RocksRecordIterator(rocksIterator);
    }

    /**
     * returns value by key searched.
     * @param key - target key
     */
    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] keys = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] vals = db.get(keys);
            if (vals == null) {
                throw new NoSuchElementException("No record found by key " + key.toString());
            }
            return ByteBuffer.wrap(vals);
        } catch (RocksDBException exc) {
            throw new IOException(exc);
        }
    }

    /**
     * executes insertion/update on record specified.
     * @param key - key that should match for attaching a value to server response
     * @param value - key-bound value
     */
    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] keys = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] vals = DAOByteOnlyConverter.readByteArray(value);
            db.put(keys, vals);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * removes record from storage.
     * @param key - target key
     */
    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(key);
            db.delete(byteArray);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * shuts up connection to data storage.
     */
    @Override
    public void close() {
        db.close();
    }

    /**
     * implements storage compaction.
     */
    @Override
    public void compact() throws IOException {
        try {
            db.compactRange();
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }
}
