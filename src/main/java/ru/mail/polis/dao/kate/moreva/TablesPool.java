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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Pool of tables used for threadsafe flushing.
* */
public class TablesPool implements Table, Closeable {

    private volatile MemTable current;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final NavigableMap<Integer, Table> writingFlushTables;
    private final BlockingQueue<FlushingTable> flushQueue;
    private final AtomicLong size;
    private final long memFlushThreshold;
    private int generation;
    private final AtomicBoolean stopFlag = new AtomicBoolean();

    /**
     * Creates pool of tables.
     * @param memFlushThreshold (max size)
     * @param startGeneration (generation number)
     * @param flushTablePool (to make the flushQueue).
     * */
    public TablesPool(final long memFlushThreshold, final int startGeneration, final int flushTablePool) {
        this.memFlushThreshold = memFlushThreshold;
        this.current = new MemTable();
        this.size = new AtomicLong(0);
        this.generation = startGeneration;
        this.writingFlushTables = new ConcurrentSkipListMap<>();
        this.flushQueue = new ArrayBlockingQueue<>(flushTablePool);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators;
        readWriteLock.readLock().lock();
        try {
            iterators = new ArrayList<>(writingFlushTables.size() + 1);
            iterators.add(current.iterator(from));
            for (final Table table : writingFlushTables.descendingMap().values()) {
                iterators.add(table.iterator(from));
            }
        } finally {
            readWriteLock.readLock().unlock();
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
        readWriteLock.readLock().lock();
        try {
            size.set(current.sizeInBytes());
            for (final Map.Entry<Integer, Table> table : writingFlushTables.entrySet()) {
                size.addAndGet(table.getValue().sizeInBytes());
            }
            return size.get();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void upsert(final ByteBuffer key, final ByteBuffer value) {
        if (stopFlag.get()) {
            throw new IllegalStateException("Already stopped");
        }
        readWriteLock.readLock().lock();
        try {
            current.upsert(key.duplicate(), value.duplicate());
        } finally {
            readWriteLock.readLock().unlock();
        }
        putIntoFlushQueue();
    }

    @Override
    public void remove(final ByteBuffer key) {
        if (stopFlag.get()) {
            throw new IllegalStateException("Already stopped");
        }
        readWriteLock.readLock().lock();
        try {
            current.remove(key.duplicate());
        } finally {
            readWriteLock.readLock().unlock();
        }
        putIntoFlushQueue();
    }

    FlushingTable takeToFlash() throws InterruptedException {
        return flushQueue.take();
    }

    void flushed(final int generation) {
        readWriteLock.writeLock().lock();
        try {
            writingFlushTables.remove(generation);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void putIntoFlushQueue() {
        if (current.sizeInBytes() > memFlushThreshold) {
            FlushingTable tableToFlush = null;
            readWriteLock.writeLock().lock();
            try {
                if (current.sizeInBytes() > memFlushThreshold) {
                    tableToFlush = new FlushingTable(current, generation);
                    writingFlushTables.put(generation, current);
                    generation++;
                    current = new MemTable();
                }
            } finally {
                readWriteLock.writeLock().unlock();
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
        readWriteLock.writeLock().lock();
        try {
            flushingTable = new FlushingTable(current, generation, true);
            writingFlushTables.put(flushingTable.getGeneration(), flushingTable.getTable());
        } finally {
            readWriteLock.writeLock().unlock();
        }
        try {
            flushQueue.put(flushingTable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
