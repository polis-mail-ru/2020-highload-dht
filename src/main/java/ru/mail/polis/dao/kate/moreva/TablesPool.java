package ru.mail.polis.dao.kate.moreva;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.Iters;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Pool of tables used for threadsafe flushing.
* */
public class TablesPool implements Table, Closeable {

    private MemTable current;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final NavigableMap<Integer, Table> writingFlushTables;
    private final BlockingQueue<FlushingTable> flushQueue;
    private final long memFlushThreshold;
    private int generation;
    private final AtomicBoolean stopFlag = new AtomicBoolean();

    /**
     * Creates pool of tables.
     *
     * @param memFlushThreshold (max size)
     * @param startGeneration   (generation number)
     * @param flushTablePool    (to make the flushQueue).
     */
    public TablesPool(final long memFlushThreshold, final int startGeneration, final int flushTablePool) {
        this.memFlushThreshold = memFlushThreshold;
        this.current = new MemTable();
        this.generation = startGeneration;
        this.writingFlushTables = new ConcurrentSkipListMap<>();
        this.flushQueue = new ArrayBlockingQueue<>(flushTablePool);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators;
        lock.readLock().lock();
        try {
            iterators = new ArrayList<>(writingFlushTables.size() + 1);
            iterators.add(current.iterator(from));
            for (final Table table : writingFlushTables.values()) {
                iterators.add(table.iterator(from));
            }
        } finally {
            lock.readLock().unlock();
        }
        final UnmodifiableIterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
        final Iterator<Cell> withoutEquals = Iters.collapseEquals(merged, Cell::getKey);

        return Iterators.filter(withoutEquals,
                cell -> {
                    assert cell != null;
                    return !cell.getValue().isTombstone();
                });
    }

    @Override
    public long sizeInBytes() {
        lock.readLock().lock();
        try {
            long size = current.sizeInBytes();
            for (final Map.Entry<Integer, Table> table : writingFlushTables.entrySet()) {
                size += table.getValue().sizeInBytes();
            }
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(final ByteBuffer key, final ByteBuffer value) {
        if (stopFlag.get()) {
            throw new IllegalStateException("Already stopped");
        }
        lock.readLock().lock();
        try {
            current.upsert(key.duplicate(), value.duplicate());
        } finally {
            lock.readLock().unlock();
        }
        putIntoFlushQueue();
    }

    @Override
    public void remove(final ByteBuffer key) {
        if (stopFlag.get()) {
            throw new IllegalStateException("Already stopped");
        }
        lock.readLock().lock();
        try {
            current.remove(key.duplicate());
        } finally {
            lock.readLock().unlock();
        }
        putIntoFlushQueue();
    }

    FlushingTable takeToFlash() throws InterruptedException {
        return flushQueue.take();
    }

    void flushed(final int generation) {
        lock.writeLock().lock();
        try {
            writingFlushTables.remove(generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void putIntoFlushQueue() {
        if (current.sizeInBytes() > memFlushThreshold) {
            FlushingTable tableToFlush = null;
            lock.writeLock().lock();
            try {
                if (current.sizeInBytes() > memFlushThreshold) {
                    tableToFlush = new FlushingTable(current, generation);
                    writingFlushTables.put(generation, current);
                    generation++;
                    current = new MemTable();
                }
            } finally {
                lock.writeLock().unlock();
            }
            if (tableToFlush != null) {
                try {
                    flushQueue.put(tableToFlush);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void close() {
        if (!stopFlag.compareAndSet(false, true)) {
            return;
        }
        FlushingTable flushingTable;
        lock.writeLock().lock();
        try {
            flushingTable = new FlushingTable(current, generation, true);
            writingFlushTables.put(flushingTable.getGeneration(), flushingTable.getTable());
        } finally {
            lock.writeLock().unlock();
        }
        try {
            flushQueue.put(flushingTable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getGeneration() {
        lock.readLock().lock();
        try {
            return generation;
        } finally {
            lock.readLock().unlock();
        }
    }
}
