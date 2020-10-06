package ru.mail.polis.dao.jhoysbou;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LsmDao implements DAO {
    private static final Logger log = LoggerFactory.getLogger(LsmDao.class);

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    @NotNull
    private final File storage;
    private final long flushThreshold;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private TableSet tableSet;

    /**
     * Simple LSM.
     *
     * @param storage        the directory to store data
     * @param flushThreshold maximum bytes to store in memory.
     *                       If a size of data gets larger, the program flushes it on drive.
     *
     * @throws IOException when where are problems with getting list of files in storage directory
     */
    public LsmDao(
            final @NotNull File storage,
            final long flushThreshold) throws IOException {
        AtomicInteger generation = new AtomicInteger();

        assert flushThreshold > 0L;
        log.info("flushThreshold = {}", flushThreshold);
        this.flushThreshold = flushThreshold;
        this.storage = storage;
        final NavigableMap<Integer, Table> ssTables = new TreeMap<>();

        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                final String name = f.getFileName().toString();
                try {
                    final int savedGeneration = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    generation.set(Math.max(generation.get(), savedGeneration));
                    ssTables.put(savedGeneration, new SSTable(f.toFile()));
                } catch (IOException e) {
                    log.error("IOException with .dat file:\n" + e.getMessage());
                } catch (NumberFormatException e) {
                    log.error("Bad file name:" + name + "\n" + e.getMessage());
                }
            });
        }
        this.tableSet = TableSet.fromFiles(ssTables, generation.get());
        log.debug("LsmDao created successfully");
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final TableSet snapshot;

        lock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }

        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size()
                + snapshot.flushingTables.size()
                + 1);

        iters.add(snapshot.memTable.iterator(from));

        for (final Table flushing : snapshot.flushingTables) {
            iters.add(flushing.iterator(from));
        }

        snapshot.ssTables.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                log.error("IOException in lsm iterator");
            }
        });
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        // One cell per key
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        // No tombstones
        final UnmodifiableIterator<Cell> alive = Iterators.filter(fresh, e -> !e.getValue().isTombstone());

        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean needsFlushing;

        lock.readLock().lock();
        try {
            this.tableSet.memTable.upsert(key, value);
            needsFlushing = this.tableSet.memTable.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needsFlushing) {
            flush();
        }
        log.debug("finished upsert key={}, value={}", key, value);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean needsFlushing;

        lock.readLock().lock();
        try {
            this.tableSet.memTable.remove(key);
            needsFlushing = this.tableSet.memTable.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needsFlushing) {
            flush();
        }
        log.debug("started remove key={}", key);
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tableSet;
            if (snapshot.memTable.size() == 0) {
                // Already flushed
                return;
            }

            this.tableSet = snapshot.flushing();
        } finally {
            lock.writeLock().lock();
        }

        final File file = new File(storage, snapshot.generation + TEMP);

        SSTable.serialize(
                file,
                snapshot.memTable.iterator(ByteBuffer.allocate(0))
        );
        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.flushed(snapshot.memTable, snapshot.generation + 1, new SSTable(dst));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        final boolean needsFlushing;
        lock.readLock().lock();
        try {
            needsFlushing = this.tableSet.memTable.size() > 0;
        } finally {
            lock.readLock().unlock();
        }

        if (needsFlushing) {
            flush();
        }

        lock.readLock().lock();
        try {
            for (final Table t : this.tableSet.ssTables.values()) {
                t.close();
            }
        } finally {
            lock.readLock().unlock();
        }
        log.debug("LsmDao successfully closed");
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();

        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }

        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size());
        final ByteBuffer empty = ByteBuffer.allocate(0);

        snapshot.ssTables.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(empty));
            } catch (IOException e) {
                log.error("IOException in lsm iterator");
            }
        });

        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        final Iterator<Cell> unique = Iters.collapseEquals(merged, Cell::getKey);

        if (!unique.hasNext()) {
            return;
        }

        lock.writeLock().lock();
        try {
            this.tableSet = snapshot.startCompaction();
        } finally {
            lock.writeLock().unlock();
        }

        final File temp = new File(storage, snapshot.generation + 1 + TEMP);
        SSTable.serialize(temp, unique);
        final File dst = new File(storage, snapshot.generation + 1 + SUFFIX);
        Files.move(temp.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet
                    .replaceCompactedFiles(snapshot.ssTables, new SSTable(dst), snapshot.generation + 1);
        } finally {
            lock.writeLock().unlock();
        }

        for (int gen : snapshot.ssTables.keySet()) {
            final Path path = new File(storage, gen + SUFFIX).toPath();
            try {
                Files.delete(path);
            } catch (NoSuchFileException e) {
                log.warn("file {} already has been deleted", path, e);
            }
        }

        log.debug("compact finished");
    }
}

