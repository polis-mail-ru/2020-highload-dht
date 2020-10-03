package ru.mail.polis.dao.impl.async;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.tables.MemTable;
import ru.mail.polis.dao.impl.tables.Table;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemTableSet implements Table {

    private final ReadWriteLock lock;
    private Table currentTable;
    private final NavigableMap<Long, Table> flushingTables;
    private final AtomicLong generation;
    private final long flushThreshold;
    private final AtomicBoolean isStopped;

    public MemTableSet(final long generation, final long flushThreshold) {
        this.lock = new ReentrantReadWriteLock();
        this.currentTable = new MemTable();
        this.flushingTables = new TreeMap<>();
        this.generation = new AtomicLong(generation);
        this.flushThreshold = flushThreshold;
        this.isStopped = new AtomicBoolean();
    }

    @Override
    public long sizeInBytes() {
        lock.readLock().lock();
        try {
            var size = currentTable.sizeInBytes();
            for (final var table : flushingTables.values()) {
                size += table.sizeInBytes();
            }
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull ByteBuffer from) throws IOException {
        lock.readLock().lock();
        final Collection<Iterator<Cell>> iterators;
        try {
            iterators = new ArrayList<>();
            iterators.add(currentTable.iterator(from));

            for (final var table : flushingTables.values()) {
                iterators.add(table.iterator(from));
            }

            final Iterator<Cell> cells = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
            final Iterator<Cell> fresh = Iters.collapseEquals(cells, Cell::getKey);
            return Iterators.filter(
                    fresh, cell -> !Objects.requireNonNull(cell).getValue().isTombstone());

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {
        if (isStopped.get()) {
            throw new IllegalStateException();
        }
        currentTable.upsert(key, value);
        if (currentTable.sizeInBytes() > flushThreshold) { // to avoid extra call & lock
            enqueueToFlush();
        }
    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {
        if (isStopped.get()) {
            throw new IllegalStateException();
        }
        currentTable.remove(key);
        if (currentTable.sizeInBytes() > flushThreshold) { // to avoid extra call & lock
            enqueueToFlush();
        }
    }

    private void enqueueToFlush() {
        lock.writeLock().lock();
        try {
            final var currentGeneration = generation.getAndIncrement();
            flushingTables.put(currentGeneration, currentTable);
            currentTable = new MemTable();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
