package ru.mail.polis.service.codearound;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TaskDAO implements DAO {

    static {
        RocksDB.loadLibrary();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RepliServiceImpl.class);
    private RocksDB db;
    File dbLocalDir;

    public TaskDAO(final RocksDB db) {
            this.db = db;
        }

    /**
    * class instance const.
     *
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
            LOGGER.error("Storage initialization failed", exc);
        }
    }

    /**
     * retrieves store record iterator.
     *
     * @param from - record to start from
     * @return value obtainable from ByteBuffer
     */
    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(from);
        rocksIterator.seek(byteArray);
        return new RocksRecordIterator(rocksIterator);
    }

    /**
     * retrives value by key searched.
     *
     * @param key - target key
     * @return value obtainable from ByteBuffer
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
     *
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
     *
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
     * resolves timestamp-featured reading data by key specified.
     *
     * @param key - key searched to read some value
     * @return value obtainable from Value instance
     */
    @Override
    public Value getValue(@NotNull final ByteBuffer key) throws IOException, NoSuchElementException {
        try {
            final byte[] value = getValueFromBytes(key);
            return Value.getValueFromBytes(value);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * commits timestamp-featured record push or modification by key specified.
     *
     * @param key - key either to add a record or to modify existing one
     * @param value - key-bound value
     */
    @Override
    public void upsertValue(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        try {
            final byte[] convertedKey = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] timestamp = Value.resolveExistingValue(value, System.currentTimeMillis()).getBytesFromValue();
            db.put(convertedKey, timestamp);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * commits timestamp-featured record deletion by key specified.
     *
     * @param key - key searched to remove specific record
     */
    @Override
    public void removeValue(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] convertedKey = DAOByteOnlyConverter.tuneBufToArray(key);
            final byte[] value = Value.resolveDeletedValue(System.currentTimeMillis()).getBytesFromValue();
            db.put(convertedKey, value);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    /**
     * implements processing for a value derived from ByteBuffer.
     *
     * @param key - key searched to remove specific record
     * @return value obtainable from byte array
     */
    private byte[] getValueFromBytes(@NotNull final ByteBuffer key) throws RocksDBException {
        final byte[] array = DAOByteOnlyConverter.tuneBufToArray(key);
        final byte[] value = db.get(array);
        if (value == null) {
            throw new NoSuchElementException("No match key found, failed request");
        }
        return value;
    }

    /**
     * Access dao and get the required key.
     *
     * @param key to specify the key to put
     * @return response containing the key
     */
    @Override
    public Response getValueWithFutures(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] value = evaluateFromBuffer(key);
            return new Response(Response.OK, value);
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    /**
     * PUT exchange runner.
     *
     * @param key - key either to add a record or to modify existing one
     * @param request - HTTP request
     */
    @Override
    public void upsertValueWithFutures(@NotNull final ByteBuffer key,
                            @NotNull final Request request) throws IOException {
        upsertValue(key, ByteBuffer.wrap(request.getBody()));
    }

    /**
     * DELETE exchange runner.
     *
     * @param key - key searched to remove specific record
     */
    @Override
    public void deleteValueWithFutures(@NotNull final ByteBuffer key) throws IOException {
        removeValue(key);
    }

    /**
     * retrieves value which is a match of key searched.
     *
     * @param key - key searched
     * @return value obtainable from a plain byte array
     */
    private byte[] evaluateFromBuffer(@NotNull final ByteBuffer key) throws IOException {
        final Value value = getValue(key);
        if (value.isValueMissing()) {
            LOGGER.error(ReplicationLsm.NOT_FOUND_ERROR_LOG);
            throw new NoSuchElementException(ReplicationLsm.NOT_FOUND_ERROR_LOG);
        }
        return value.getBytesFromValue();
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
