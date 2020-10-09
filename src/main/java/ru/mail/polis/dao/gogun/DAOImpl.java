package ru.mail.polis.dao.gogun;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import javax.annotation.Nullable;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class DAOImpl implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private TableSet tables;

    @NotNull
    private final BlockingQueue<FlushTask> queue;
    @NotNull
    private final Thread flusher;

    @NotNull
    private final File storage;
    private final long flushThreshold;

    private static final Logger logger = LoggerFactory.getLogger(DAOImpl.class);

    /**
     * implementation of lsm.
     *
     * @param storage        directory with sstables
     * @param flushThreshold threshold to write data
     * @throws IOException io error
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold, final int executorQueueSize) throws IOException {
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                try {
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    ssTables.put(gen, new SSTable(f.toFile()));
                } catch (IOException e) {
                    logger.error("ctor bug", e);
                } catch (NumberFormatException e) {
                    logger.error("bad name", e);
                }
            });
        }

        ssTables.entrySet()
                .stream()
                .max(Entry.comparingByKey())
                .ifPresentOrElse(
                        generation -> this.tables = TableSet.fromFiles(ssTables, generation.getKey() + 1),
                        () -> this.tables = TableSet.fromFiles(ssTables, 1));

        this.queue = new ArrayBlockingQueue<>(executorQueueSize);
        this.flusher = new Flusher();
        this.flusher.start();
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
        final List<Iterator<Row>> iters = new ArrayList<>(snapshot.ssTables.size()
                + snapshot.flushing.size() + 1);
        iters.add(snapshot.memTable.iterator(from));

        snapshot.flushing.forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                logger.error("iter fail", e);
            }
        });

        snapshot.ssTables.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                logger.error("iter fail", e);
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

        for (final int generation : snapshot.ssTables.keySet()) {
            final File fileToDelete = new File(storage, generation + SUFFIX);
            Files.delete(fileToDelete.toPath());
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

    private void flush() {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tables;
            if (snapshot.memTable.getSize() == 0) {
                return;
            }
            this.tables = snapshot.startFlushing();

        } finally {
            lock.writeLock().unlock();
        }

        try {
            this.queue.put(new FlushTask(snapshot.memTable, snapshot.generation));
        } catch (InterruptedException e) {
            logger.error("cant get task", e);
        }


    }

    private final static class FlushTask {
        private final int generation;
        @Nullable
        private final MemTable memTable;

        private FlushTask(final MemTable mem, final int gen) {
            this.memTable = mem;
            this.generation = gen;
        }

        private boolean isPoison() {
            return memTable == null;
        }

    }

    private final class Flusher extends Thread {
        public Flusher() {
            super("flusher");
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final FlushTask task = queue.take();

                    if (task.isPoison()) {
                        return;
                    }

                    logger.info("Flushing {} bytes(s) to {}", task.memTable.getSizeInBytes(), task.generation);

                    final SSTable table = serialize(task.generation, task.memTable.iterator(ByteBuffer.allocate(0)));
                    lock.writeLock().lock();
                    try {
                        tables = tables.flushed(task.memTable, table, task.generation);
                    } finally {
                        lock.writeLock().unlock();
                    }

                    logger.info("Flushed {} bytes(s) to {}", task.memTable.getSizeInBytes(), task.generation);

                } catch (Exception e) {
                    logger.debug("Cant get task");
                    Runtime.getRuntime().halt(-1);
                }
            }
        }
    }

    private SSTable serialize(final int generation, final Iterator<Row> iterator) throws IOException {
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, iterator);
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        return new SSTable(dst);
    }


    @Override
    public void close() throws IOException {
        boolean isFlushing;
        lock.readLock().lock();
        try {
            isFlushing = tables.memTable.getSize() > 0;
        } finally {
            lock.readLock().unlock();
        }

        if (isFlushing) {
            flush();
            try {
                queue.put(new FlushTask(null, 0));
                flusher.join();
            } catch (Exception e) {
                logger.error("cant stop correctly", e);
            }
        }

        lock.readLock().lock();
        try {
            for (final Entry<Integer, SSTable> elem : tables.ssTables.entrySet()) {
                elem.getValue().close();
            }
        } finally {
            lock.readLock().unlock();
        }


    }
}
