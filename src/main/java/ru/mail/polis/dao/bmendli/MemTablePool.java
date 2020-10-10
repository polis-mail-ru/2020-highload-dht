package ru.mail.polis.dao.bmendli;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.Iters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemTablePool implements Table {

    private final Logger log = LoggerFactory.getLogger(MemTablePool.class);
    private final ReadWriteLock lock;
    private final NavigableMap<Integer, Table> waitingFlushTables;
    private final BlockingQueue<FlushTable> flushTableQueue;
    private final AtomicBoolean stopped;
    private final long flushThreshold;

    private MemTable currentMemTable;
    private int generation;

    /**
     * Pool of flushing tables and current memory table.
     */
    public MemTablePool(final long flushThreshold,
                        final int firstGeneration,
                        final int flushTablePool) {
        this.lock = new ReentrantReadWriteLock();
        this.flushTableQueue = new ArrayBlockingQueue<>(flushTablePool);
        this.stopped = new AtomicBoolean();
        this.flushThreshold = flushThreshold;
        this.generation = firstGeneration;
        this.currentMemTable = new MemTable();
        this.waitingFlushTables = new TreeMap<>();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       final long expireTime) {
        if (stopped.get()) {
            final String errorMsg = "cannot upsert data";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        lock.writeLock().lock();
        try {
            currentMemTable.upsert(key, value, expireTime);
        } finally {
            lock.writeLock().unlock();
        }
        enqueueToFlush();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        if (stopped.get()) {
            final String errorMsg = "cannot remove data";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        lock.writeLock().lock();
        try {
            currentMemTable.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
        enqueueToFlush();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators;
        lock.readLock().lock();
        try {
            iterators = new ArrayList<>(waitingFlushTables.size() + 1);
            iterators.add(currentMemTable.iterator(from));
            waitingFlushTables.values().forEach(table -> {
                try {
                    iterators.add(table.iterator(from));
                } catch (IOException e) {
                    log.error("error get iterator", e);
                }
            });
        } finally {
            lock.readLock().unlock();
        }

        final Iterator<Cell> mergedCellIterator = Iterators.mergeSorted(iterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        final Iterator<Cell> withoutEqualsIterator = Iters.collapseEquals(mergedCellIterator, Cell::getKey);
        return Iterators.filter(withoutEqualsIterator,
                cell -> !cell.getValue().isTombstone() && !cell.getValue().isExpired());
    }

    @Override
    public long size() {
        lock.readLock().lock();
        try {
            long tempSize = currentMemTable.size();
            for (final Map.Entry<Integer, Table> entry : waitingFlushTables.entrySet()) {
                tempSize += entry.getValue().size();
            }
            return tempSize;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        final FlushTable flushTable;
        lock.writeLock().lock();
        try {
            flushTable = new FlushTable(generation, currentMemTable, true);
        } finally {
            lock.writeLock().unlock();
        }
        try {
            flushTableQueue.put(flushTable);
        } catch (InterruptedException e) {
            log.error("Error. Cannot put FlushTable {} in blocking queue", flushTable, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Return generation of {@code currentMemTable}.
     */
    public int getGeneration() {
        lock.readLock().lock();
        try {
            return generation;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove table which is flushed.
     */
    public void flushed(final int generation) {
        lock.writeLock().lock();
        try {
            waitingFlushTables.remove(generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public FlushTable takeTableToFlush() throws InterruptedException {
        return flushTableQueue.take();
    }

    private void enqueueToFlush() {
        if (currentMemTable.size() > flushThreshold) {
            FlushTable flushTable = null;
            lock.writeLock().lock();
            try {
                if (currentMemTable.size() > flushThreshold) {
                    flushTable = new FlushTable(generation, currentMemTable);
                    waitingFlushTables.put(generation, currentMemTable);
                    ++generation;
                    currentMemTable = new MemTable();
                }
            } finally {
                lock.writeLock().unlock();
            }
            if (flushTable != null) {
                try {
                    flushTableQueue.put(flushTable);
                } catch (InterruptedException e) {
                    log.error("Error. Cannot put FlushTable {} in blocking queue", flushTable, e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
