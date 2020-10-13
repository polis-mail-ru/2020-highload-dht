package ru.mail.polis.dao.s3ponia;

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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PersistenceDAO implements DAO {
    private final DiskManager manager;
    private final long maxMemory;
    private final AtomicLong currMemory = new AtomicLong();
    private static final long MIN_FREE_MEMORY = 64 * 1024;


    private final ReentrantReadWriteLock readWriteLock =
            new ReentrantReadWriteLock();

    @GuardedBy("readWriteLock")
    private TableSet tableSet;

    private PersistenceDAO(final File data, final long maxMemory) throws IOException {
        this.manager = new DiskManager(Paths.get(data.getAbsolutePath(),
                DiskManager.META_PREFIX + data.getName() + DiskManager.META_EXTENSION));
        final NavigableMap<Integer, Table> diskSet = new TreeMap<>();
        for (var diskTable : this.manager.diskTables()) {
            diskSet.put(diskTable.getGeneration(), diskTable);
        }
        // Closing all files and clear list of diskTables and fileNames
        this.manager.close();
        this.tableSet = new TableSet(
                new MemTable(this.manager.getGeneration()),
                Collections.emptySet(),
                diskSet,
                this.manager.getGeneration()
        );
        this.maxMemory = maxMemory;
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        readWriteLock.writeLock().lock();
        try {
            snapshot = this.tableSet;
            if (snapshot.memTable.size() == 0L) {
                return;
            }
            this.tableSet = snapshot.beforeFlush();
        } finally {
            readWriteLock.writeLock().unlock();
        }

        this.manager.save(snapshot.memTable.iterator(ByteBuffer.allocate(0)), snapshot.generation);

        var dest = this.manager.diskTableFromGeneration(snapshot.generation);

        readWriteLock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.afterFlush(snapshot.memTable, dest, snapshot.generation);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public static PersistenceDAO of(final File data, final long memorySize) throws IOException {
        return new PersistenceDAO(data, memorySize);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        readWriteLock.writeLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        final var diskIterators = new ArrayList<Iterator<Table.ICell>>();
        diskIterators.add(snapshot.memTable.iterator(from));
        snapshot.diskTables.forEach((a, table) -> diskIterators.add(table.iterator(from)));
        snapshot.flushingTables.forEach(table -> diskIterators.add(table.iterator(from)));
        final var merge = Iterators.mergeSorted(diskIterators, Table.ICell::compareTo);
        final var newest = Iters.collapseEquals(merge, Table.ICell::getKey);
        final var removeDead = Iterators.filter(newest, el -> !el.getValue().isDead());

        return Iterators.transform(removeDead, c -> Record.of(c.getKey(), c.getValue().getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean flushPending;
        readWriteLock.writeLock().lock();
        try {
            this.tableSet.memTable.upsert(key, value);
            currMemory.addAndGet(key.limit() + value.limit() + Long.BYTES + Integer.BYTES);
            flushPending = Math.abs(maxMemory - currMemory.get()) <= MIN_FREE_MEMORY || currMemory.get() >= maxMemory;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        if (flushPending) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean flushPending;
        readWriteLock.writeLock().lock();
        try {
            this.tableSet.memTable.remove(key);
            currMemory.addAndGet(key.limit() + Long.BYTES + Integer.BYTES);
            flushPending = Math.abs(maxMemory - currMemory.get()) <= MIN_FREE_MEMORY || currMemory.get() >= maxMemory;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        if (flushPending) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void compact() throws IOException {
        final TableSet snapshot;
        readWriteLock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            readWriteLock.readLock().unlock();
        }

        final var point = ByteBuffer.allocate(0);
        final var iterators = new ArrayList<Iterator<Table.ICell>>();
        iterators.add(snapshot.memTable.iterator(point));
        snapshot.diskTables.forEach((a, table) -> iterators.add(table.iterator(point)));
        final var merge = Iterators.mergeSorted(iterators, Table.ICell::compareTo);
        final var newest = Iters.collapseEquals(merge, Table.ICell::getKey);

        readWriteLock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.beforeCompact();
        } finally {
            readWriteLock.writeLock().unlock();
        }

        this.manager.clear();
        this.manager.save(newest, snapshot.generation);
        final var dest = this.manager.diskTableFromGeneration(snapshot.generation);

        readWriteLock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.afterCompact(snapshot.diskTables, dest, snapshot.generation);
        } finally {
            readWriteLock.writeLock().unlock();
        }

        for (final var diskTable : snapshot.diskTables.values()) {
            diskTable.close();
            Files.delete(((DiskTable) diskTable).getFilePath());
        }
    }
}
