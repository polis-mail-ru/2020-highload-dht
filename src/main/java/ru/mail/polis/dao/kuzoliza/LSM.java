package ru.mail.polis.dao.kuzoliza;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LSM implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    private static final Logger log = LoggerFactory.getLogger(LSM.class);

    private TableSet tables;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService executor;

    // Data
    private final File storage;
    private final long flushThreshold;
    // State
    private int generation;

    /**
     * Key Value DB.
     *
     * @param storage data storage
     * @param flushThreshold memTable size (when size == flushThreshold memTable is converted into ssTable)
     * @throws IOException may appear exception
     */
    public LSM(@NotNull final File storage, final long flushThreshold) throws IOException {
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        assert flushThreshold > 0L;
        generation = -1;
        final NavigableMap<Integer, SStable> ssTables = new TreeMap<>();

        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> {
                final String name = path.getFileName().toString();
                return name.endsWith(SUFFIX) && !path.toFile().isDirectory()
                        && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$");
            }).forEach(f -> {
                try {
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    generation = Math.max(generation, gen);
                    ssTables.put(gen, new SStable(f.toFile()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        this.tables = TableSet.fromFiles(ssTables, generation + 1);
        this.executor = Executors.newCachedThreadPool();
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final Iterator<Cell> alive = noTombstones(from);
        return Iterators.transform(alive, e -> {
            assert e != null;
            return Record.of(e.getKey(), e.getValue().getData().rewind());
        });

    }

    private Iterator<Cell> noTombstones(@NotNull final ByteBuffer from) throws IOException {

        final TableSet snapshot;

        lock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            lock.readLock().unlock();
        }

        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.files.size() + 1);
        iters.add(snapshot.mem.iterator(from));
        snapshot.files.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        for (final Table flushing : snapshot.flushing) {
            iters.add(flushing.iterator(from));
        }
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        // One cell per key
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        // No tombstones
        return Iterators.filter(fresh, e -> {
            assert e != null;
            return e.getValue().getData() != null;
        });
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> alive = noTombstones(ByteBuffer.allocate(0));

        final TableSet snapshot;
        final File file;

        final File dst;

        lock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            lock.readLock().unlock();
        }

        // switch generation
        lock.writeLock().lock();
        try {
            this.tables = this.tables.startCompacting();
        } finally {
            lock.writeLock().unlock();
        }

        file = new File(storage, snapshot.generation + TEMP);
        SStable.serialize(file, alive);

        dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        //Switch
        lock.writeLock().lock();
        try {
            this.tables = this.tables.replaceCompactedFiles(snapshot.files, new SStable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }

        for (final int k : snapshot.files.keySet()) {
            final File removeFile = new File(storage, k + SUFFIX);
            Files.delete(removeFile.toPath());
        }

        snapshot.generation = 0;
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {

        final boolean needFlushing;
        lock.readLock().lock();
        try {
            tables.mem.upsert(key, value);
            needFlushing = tables.mem.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needFlushing) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {

        final boolean needFlushing;
        lock.readLock().lock();
        try {
            tables.mem.remove(key);
            needFlushing = tables.mem.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }

        if (needFlushing) {
            flush();
        }
    }

    private void flush() {
        // Dump memTable
        executor.execute(() -> {
            final TableSet snapshot;
            final File file;
            lock.writeLock().lock();

            try {
                snapshot = this.tables;
                if (snapshot.mem.size() == 0) {
                    return;
                }
                this.tables = snapshot.markedAsFlushing();
                file = new File(storage, snapshot.generation + TEMP);
                try {
                    SStable.serialize(file, snapshot.mem.iterator(ByteBuffer.allocate(0)));
                } catch (IOException e) {
                    log.error("Can't write file", e);
                }
            } finally {
                lock.writeLock().unlock();
            }

            try {
                moveAndSwitch(file, snapshot);
            } catch (IOException e) {
                log.error("Can't move files", e);
            }
        });

    }

    private void moveAndSwitch(final File file, final TableSet snapshot) throws IOException {
        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        //Switch
        lock.writeLock().lock();
        try {
            this.tables = this.tables.flushed(snapshot.mem, new SStable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {

        final boolean needFlushing;
        lock.readLock().lock();
        try {
            needFlushing = tables.mem.size() > 0;
        } finally {
            lock.readLock().unlock();
        }

        if (needFlushing) {
            flush();
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Can't shutdown executor", e);
            Thread.currentThread().interrupt();
        }
        tables.files.values().forEach(SStable::close);
    }
}
