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
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemTablePool implements Table {

    private final Logger logger = LoggerFactory.getLogger(MemTablePool.class);
    private final ReadWriteLock readWriteLock;
    private final NavigableMap<Integer, Table> waitingFlushTables;
    private final BlockingQueue<FlushTable> blockingQueue;
    private final AtomicLong tempSize;
    private final AtomicBoolean stop;
    private final long flushThreshold;

    private volatile MemTable currentMemTable;
    private int generation;

    /**
     * Pool of flushing tables and current memory table.
     */
    public MemTablePool(final long flushThreshold,
                        final int firstGeneration,
                        final int flushTablePool) {
        this.readWriteLock = new ReentrantReadWriteLock();
        this.blockingQueue = new ArrayBlockingQueue<>(flushTablePool);
        this.tempSize = new AtomicLong(0);
        this.stop = new AtomicBoolean();
        this.flushThreshold = flushThreshold;
        this.generation = firstGeneration;
        this.currentMemTable = new MemTable();
        this.waitingFlushTables = new TreeMap<>();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       final long expireTime) {
        if (stop.get()) {
            logger.error("cannot upsert data");
            throw new IllegalStateException();
        }
        currentMemTable.upsert(key, value, expireTime);
        enqueueToFlush();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        if (stop.get()) {
            logger.error("cannot remove data");
            throw new IllegalStateException();
        }
        currentMemTable.remove(key);
        enqueueToFlush();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators;
        readWriteLock.readLock().lock();
        try {
            iterators = new ArrayList<>(waitingFlushTables.size() + 1);
            iterators.add(currentMemTable.iterator(from));
            waitingFlushTables.descendingMap().values().forEach(table -> {
                try {
                    iterators.add(table.iterator(from));
                } catch (IOException e) {
                    logger.error("error get iterator", e);
                }
            });
        } finally {
            readWriteLock.readLock().unlock();
        }

        final Iterator<Cell> mergedCellIterator = Iterators.mergeSorted(iterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        final Iterator<Cell> withoutEqualsIterator = Iters.collapseEquals(mergedCellIterator, Cell::getKey);
        return Iterators.filter(withoutEqualsIterator,
                cell -> !cell.getValue().isTombstone() && !cell.getValue().isExpired());
    }

    @Override
    public long size() {
        readWriteLock.readLock().lock();
        try {
            tempSize.set(currentMemTable.size());
            waitingFlushTables.forEach((key, value) -> tempSize.addAndGet(value.size()));
            return tempSize.get();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        if (!stop.compareAndSet(false, true)) {
            return;
        }
        final FlushTable flushTable;
        readWriteLock.writeLock().lock();
        try {
            flushTable = new FlushTable(generation, currentMemTable, true);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        try {
            blockingQueue.put(flushTable);
        } catch (InterruptedException e) {
            logger.error("error put flush table {} int blocking queue", flushTable, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return generation of {@code currentMemTable}
     */
    public int getGeneration() {
        readWriteLock.readLock().lock();
        try {
            return generation;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void flushed(final int generation) {
        readWriteLock.writeLock().lock();
        try {
            waitingFlushTables.remove(generation);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public FlushTable takeTableToFlush() throws InterruptedException {
        return blockingQueue.take();
    }

    private void enqueueToFlush() {
        if (currentMemTable.size() > flushThreshold) {
            FlushTable flushTable = null;
            readWriteLock.writeLock().lock();
            try {
                if (currentMemTable.size() > flushThreshold) {
                    flushTable = new FlushTable(generation, currentMemTable);
                    waitingFlushTables.put(generation, currentMemTable);
                    ++generation;
                    currentMemTable = new MemTable();
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
            if (flushTable != null) {
                try {
                    blockingQueue.put(flushTable);
                } catch (InterruptedException e) {
                    logger.error("error put flush table {} int blocking queue", flushTable, e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
