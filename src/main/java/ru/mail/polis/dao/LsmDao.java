package ru.mail.polis.dao;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LsmDao implements DAO {
    private static final Logger log = LoggerFactory.getLogger(LsmDao.class);

    private static String SUFFIX = ".dat";
    private static String TEMP = ".tmp";

    @NotNull
    private final File storage;
    private final long flushThreshold;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    @GuardedBy("lock")
    private TableSet tableSet;
    int maxGeneration;

    @NotNull
    private final BlockingQueue<FlushTask> flushTasks;
    @NotNull
    private final Thread flusher;

    /**
     * Creates LsmDao.
     * @param storage database file storage
     * @param flushThreshold maximum size in bytes, data will be written to a file if size in bytes > flushThreshold
     * @throws IOException if an I/O error occurs
     */
    public LsmDao(@NotNull final File storage, final long flushThreshold, final int flushQueueSize) throws IOException {
        assert flushThreshold > 0L;
        assert flushQueueSize > 0L;
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        final NavigableMap<Integer, Table> ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                try {
                    // 3.dat
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    maxGeneration = Math.max(maxGeneration, gen);
                    ssTables.put(gen, new SSTable(f.toFile()));
                } catch (IOException e) {
                    // Log bad file
                    log.warn("Exception in 'new SSTable' ", e);
                } catch (NumberFormatException e) {
                    log.info("Incorrect file name: " + f.getFileName().toString());
                }
            });
        }
        this.tableSet = TableSet.fromFiles(ssTables, maxGeneration + 1);

        // Flusher
        this.flushTasks = new ArrayBlockingQueue<>(flushQueueSize);
        this.flusher = new Flusher();
        flusher.start();
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
        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size() + 1);
        iters.add(snapshot.memTable.iterator(from));
        for (final Table flushing : snapshot.flushingTables) {
            iters.add(flushing.iterator(from));
        }
        for (final Table t : snapshot.ssTables.descendingMap().values()) {
            iters.add(t.iterator(from));
        }
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        // One cell per key
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        // No tombstones
        final Iterator<Cell> alive = Iterators.filter(fresh, e -> !e.getValue().isTombstone());
        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean needsFlushing;
        lock.readLock().lock();
        try {
            tableSet.memTable.upsert(key.asReadOnlyBuffer(), value.asReadOnlyBuffer());
            needsFlushing = tableSet.memTable.sizeInBytes() > flushThreshold;
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
            tableSet.memTable.remove(key.asReadOnlyBuffer());
            needsFlushing = tableSet.memTable.sizeInBytes() > flushThreshold;
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
            snapshot = this.tableSet;
            if (snapshot.memTable.size() == 0L) {
                // Nothing to flush
                return;
            }

            this.tableSet = snapshot.markMemTableAsFlushing();
        } finally {
            lock.writeLock().unlock();
        }

        try {
            this.flushTasks.put(new FlushTask(snapshot.memTable, snapshot.generation));
        } catch (Exception e) {
            throw new IOException("Can't enque flush task", e);
        }
    }

    @Override
    public void close() throws IOException {
        flush();

        for (final Table t : this.tableSet.ssTables.values()) {
            t.close();
        }

        try {
            flushTasks.put(new FlushTask(null, 0));
            flusher.join();
        } catch (Exception e) {
            throw new RuntimeException("Can't stop flusher", e);
        }
    }

    private Iterator<Cell> aliveCells(@NotNull final ByteBuffer from, @NotNull final TableSet snapshot) throws IOException {
        final List<Iterator<Cell>> iterators = new ArrayList<>();
        for (final Table ssTable : snapshot.ssTables.values()) {
            iterators.add(ssTable.iterator(from));
        }

        final Iterator<Cell> cellIterator = Iters.collapseEquals(
                Iterators.mergeSorted(iterators, Cell.COMPARATOR),
                Cell::getKey
        );

        return Iterators.filter(
                cellIterator, cell -> {
                    assert cell != null;
                    return !cell.getValue().isTombstone();
                }
        );
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tableSet;
            this.tableSet = this.tableSet.startCompacting();
        } finally {
            lock.writeLock().unlock();
        }


        final File tmp = new File(storage, "compact" + TEMP);
        SSTable.serialize(tmp, aliveCells(ByteBuffer.allocate(0), snapshot));

        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(tmp.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        // Replace old files

        for (int gen : snapshot.ssTables.keySet()) {
            Files.delete(Path.of(storage.toString() + "/" + gen + SUFFIX));
        }
        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.replaceCompactedFiles(snapshot.ssTables, new SSTable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private final static class FlushTask {
        @Nullable
        private final Table mem; // null if poison pill
        private final int generation;

        public FlushTask(Table mem, int generation) {
            this.mem = mem;
            this.generation = generation;
        }

        private boolean isPoisonPill() {
            return mem == null;
        }
    }

    private final class Flusher extends Thread {
        public Flusher() {
            super("flusher");
        }

        @Override
        public void run() {
            try {
                while (true) {
                    final FlushTask task = flushTasks.take();

                    if (task.isPoisonPill()) {
                        return;
                    }

                    // Dump memTable
                    final File file = new File(storage, task.generation + TEMP);
                    SSTable.serialize(file, task.mem.iterator(ByteBuffer.allocate(0)));
                    final File dst = new File(storage, task.generation + SUFFIX);
                    Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

                    lock.writeLock().lock();
                    try {
                        tableSet = tableSet.moveFlushingMemTableToFlushedFiles(
                            task.mem,
                            new SSTable(dst),
                            task.generation);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } catch (Exception e) {

            }
        }
    }
}
