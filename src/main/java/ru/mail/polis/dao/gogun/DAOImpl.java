package ru.mail.polis.dao.gogun;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DAOImpl implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    @GuardedBy("lock")
    private TableSet tables;

    @NotNull
    private final File storage;
    private final long flushThreshold;

    private static final Logger logger = Logger.getLogger(DAOImpl.class.getName());
    private int maxGeneration = 0;

    /**
     * implementation of lsm.
     *
     * @param storage        directory with sstables
     * @param flushThreshold threshold to write data
     * @throws IOException io error
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold) throws IOException {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.flushThreshold = flushThreshold;
//        this.memTable = new MemTable();
        final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                try {
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    this.maxGeneration = Math.max(maxGeneration, gen);
                    ssTables.put(gen, new SSTable(f.toFile()));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "ctor bug", e);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "bad name", e);
                }
            });
        }
        this.tables = TableSet.fromFiles(ssTables, maxGeneration + 1);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final Iterator<Row> fresh = rowIterator(from);
        final Iterator<Row> alive = Iterators.filter(fresh, e -> !e.getValue().isTombstone());

        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @NotNull
    private Iterator<Row> rowIterator(@NotNull final ByteBuffer from) throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            lock.readLock().unlock();
        }
        final List<Iterator<Row>> iters = new ArrayList<>(snapshot.ssTables.size() + 1);
        iters.add(snapshot.memTable.iterator(from));

        snapshot.flushing.forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "iter fail", e);
            }
        });

        snapshot.ssTables.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "iter fail", e);
            }
        });

        final Iterator<Row> merged = Iterators.mergeSorted(iters, Row.COMPARATOR);

        return Iters.collapseEquals(merged, Row::getKey);
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            lock.readLock().unlock();
        }
        final ByteBuffer from = ByteBuffer.allocate(0);
        final Collection<Iterator<Row>> iterators = new ArrayList<>(snapshot.ssTables.size());
        for (final Table file : snapshot.ssTables.descendingMap().values()) {
            iterators.add(file.iterator(from));
        }

        final Iterator<Row> merged = Iterators.mergeSorted(iterators, Row.COMPARATOR);

        final Iterator<Row> iterator = Iters.collapseEquals(merged, Row::getKey);
        if (!iterator.hasNext()) {
            return;
        }
        lock.writeLock().lock();
        try {
            this.tables = this.tables.startCompaction();
        } finally {
            lock.writeLock().unlock();
        }

        final File file = new File(storage, snapshot.generation + TEMP);
        SSTable.serialize(file, iterator);
        final File db = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), db.toPath(), StandardCopyOption.ATOMIC_MOVE);
        lock.writeLock().lock();
        try {
            this.tables = this.tables.replaceCompactedFiles(snapshot.ssTables, new SSTable(db), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }

        for (int generation : snapshot.ssTables.keySet()) {
            final File fileToDelete = new File(storage, generation + SUFFIX);
            if (!fileToDelete.delete()) {
                throw new IOException("Unable to remove " + fileToDelete);
            }
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean needsFlushing;
        lock.readLock().lock();
        try {
            tables.memTable.upsert(key, value);
            needsFlushing = tables.memTable.getSizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needsFlushing) {
            flush();
        }

    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean needsFlushing;
        lock.readLock().lock();
        try {
            tables.memTable.remove(key);
            needsFlushing = tables.memTable.getSizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needsFlushing) {
            flush();
        }
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tables;
            if (snapshot.memTable.getSize() == 0) {
                return;
            }
            this.tables = snapshot.flushing();

        } finally {
            lock.writeLock().unlock();
        }
        final File file = new File(storage, snapshot.generation + TEMP);
        SSTable.serialize(
                file,
                snapshot.memTable.iterator(ByteBuffer.allocate(0)));
        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            this.tables = this.tables.flushed(snapshot.memTable, new SSTable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (tables.memTable.getSize() > 0) {
            flush();
        }
        for (final Entry<Integer, SSTable> elem : tables.ssTables.entrySet()) {
            elem.getValue().close();
        }
    }
}
