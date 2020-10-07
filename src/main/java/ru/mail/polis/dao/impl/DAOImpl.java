package ru.mail.polis.dao.impl;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.tables.SSTable;
import ru.mail.polis.dao.impl.tables.Table;
import ru.mail.polis.dao.impl.tables.TableSet;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class DAOImpl implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    @NotNull
    private final File storage;
    private final long flushThreshold;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    @NotNull
    @GuardedBy("lock")
    private TableSet tableSet;
    @NotNull
    private final ExecutorService executor;

    /**
     * Creates persistent DAO.
     *
     * @param storage folder to save and read data from
     * @param flushThreshold count of flushing tables
     * @param pools count of async threads
     * @throws IOException if cannot open or read SSTables
     */
    public DAOImpl(@NotNull final File storage,
                   final long flushThreshold,
                   final int pools) throws IOException {
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        final NavigableMap<Long, Table> ssTables = new TreeMap<>();
        final AtomicLong maxGeneration = new AtomicLong();
        try (Stream<Path> stream = Files.walk(storage.toPath(), 1)) {
            stream.filter(path -> {
                final String name = path.getFileName().toString();
                return name.endsWith(SUFFIX)
                        && !path.toFile().isDirectory()
                        && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$"); })
                    .forEach(path -> {
                        try {
                            final String name = path.getFileName().toString();
                            final long currentGeneration = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            maxGeneration.set(Math.max(maxGeneration.get(), currentGeneration));
                            ssTables.put(currentGeneration, new SSTable(path.toFile()));
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        }
        maxGeneration.set(maxGeneration.get() + 1);
        this.tableSet = TableSet.fromFiles(ssTables, maxGeneration.get());
        this.executor = Executors.newFixedThreadPool(pools);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(cellIterator(from),
                cell -> Record.of(Objects.requireNonNull(cell).getKey(), cell.getValue().getData()));
    }

    /**
     * Create an iterator over alive {@link Cell}.
     *
     * @param from data on which iterator is created
     * @return an iterator over alive cells
     */
    @NotNull
    public Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }
        final List<Iterator<Cell>> fileIterators = new ArrayList<>(snapshot.ssTables.size() + 1);
        fileIterators.add(snapshot.memTable.iterator(from));
        snapshot.flushing.forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        final Iterator<Cell> fresh = freshCellIterators(snapshot, from, fileIterators);
        return Iterators.filter(
                fresh, cell -> !Objects.requireNonNull(cell).getValue().isTombstone());
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean isToFlush;
        lock.readLock().lock();
        try {
            tableSet.memTable.upsert(key, value);
            isToFlush = tableSet.memTable.sizeInBytes() >= flushThreshold;

        } finally {
            lock.readLock().unlock();
        }
        if (isToFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean isToFlush;
        lock.readLock().lock();
        try {
            tableSet.memTable.remove(key);
            isToFlush = tableSet.memTable.sizeInBytes() >= flushThreshold;

        } finally {
            lock.readLock().unlock();
        }
        if (isToFlush) {
            flush();
        }
    }

    @Override
    public void close() {
        boolean isReadyToFlush;
        lock.readLock().lock();
        try {
            isReadyToFlush = tableSet.memTable.sizeInBytes() > 0;
        } finally {
            lock.readLock().unlock();
        }
        if (isReadyToFlush) {
            flush();
        }
        executor.shutdown();
        while (true) {
            if (executor.isTerminated()) {
                break;
            }
        }
        lock.readLock().lock();
        try {
            tableSet.ssTables.values().forEach(Table::close);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void flush() {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tableSet;
            if (snapshot.memTable.sizeInBytes() == 0L) {
                return;
            }
            this.tableSet = snapshot.markAsFlushing();
        } finally {
            lock.writeLock().unlock();
        }

        executor.execute(() -> {
            try {
                final File file = serialize(snapshot.generation, snapshot.memTable.iterator(ByteBuffer.allocate(0)));
                lock.writeLock().lock();
                try {
                    tableSet = tableSet.moveToFlushedFiles(snapshot.memTable, new SSTable(file), snapshot.generation);
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

    }

    @Override
    public synchronized void compact() throws IOException {
        boolean isEmptyListOfFiles;
        lock.readLock().lock();
        try {
            isEmptyListOfFiles = tableSet.ssTables.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
        if (isEmptyListOfFiles) {
            return;
        }
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }

        final ByteBuffer from = ByteBuffer.allocate(0);
        final List<Iterator<Cell>> fileIterators = new ArrayList<>(snapshot.ssTables.size());
        final Iterator<Cell> fresh = freshCellIterators(snapshot, from, fileIterators);

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.compacting();
        } finally {
            lock.writeLock().unlock();
        }

        final File file = serialize(snapshot.generation, fresh);
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(f -> {
                final String name = f.getFileName().toFile().toString();
                return Integer.parseInt(name.substring(0, name.indexOf('.'))) < snapshot.generation;
            }).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.replaceCompactedFiles(snapshot.ssTables,
                    new SSTable(file),
                    snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Iterator<Cell> freshCellIterators(@NotNull final TableSet snapshot,
                                              @NotNull final ByteBuffer from,
                                              @NotNull final List<Iterator<Cell>> fileIterators) {
        snapshot.ssTables.descendingMap().values().forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        final Iterator<Cell> merged = Iterators.mergeSorted(fileIterators, Comparator.naturalOrder());
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    private File serialize(final long generation,
                           final Iterator<Cell> iterator) throws IOException {
        final File temp = new File(storage, generation + TEMP);
        SSTable.write(iterator, temp);
        final File file = new File(storage, generation + SUFFIX);
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        return file;
    }
}
