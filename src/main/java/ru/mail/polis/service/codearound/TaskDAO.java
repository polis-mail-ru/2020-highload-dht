package ru.mail.polis.service.codearound;
import static java.lang.Byte.MIN_VALUE;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
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

        File dbLocalDir;
        private RocksDB db;

        public TaskDAO(RocksDB db) {
            this.db = db;
        }
        public TaskDAO(@NotNull final File data) {
            final Options opts = new Options();
            opts.setCreateIfMissing(true);
            dbLocalDir = data;
            try {
                Files.createDirectories(dbLocalDir.getParentFile().toPath());
                Files.createDirectories(dbLocalDir.getAbsoluteFile().toPath());
                db = RocksDB.open(opts, dbLocalDir.getAbsolutePath());
            } catch (IOException | RocksDBException exc) {
                System.out.println("Error initializing DB instance in local file system - DB access can't be provided\n");
            }
            System.out.println("DB initializing finished - storage function enabled\n");
        }

    /**
     * Getting a byte array.
     *
     * @param buffer - final ByteBuffer
     * @return byte array
     */

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final RocksIterator rocksIterator = db.newIterator();
        final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(from);
        rocksIterator.seek(byteArray);
        return new RocksRecordIterator(rocksIterator);
    }
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
    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] byteArray = DAOByteOnlyConverter.tuneBufToArray(key);
            db.delete(byteArray);
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }
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
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }
    @Override
    public void close() {
        db.close();
    }
}