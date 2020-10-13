package ru.mail.polis.dao.impl;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Cell;
import ru.mail.polis.MemTable;
import ru.mail.polis.Record;
import ru.mail.polis.SSTable;
import ru.mail.polis.Table;
import ru.mail.polis.Value;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.TableSet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNull;

public final class DAOImpl implements DAO {

    private static final String SUFFIX = "SSTable.dat";
    private static final String TEMP = "SSTable.tmp";
    private final long flushThreshold;
    private final File file;

    private TableSet tables;
    final NavigableMap<Integer, Table> ssTables = new ConcurrentSkipListMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService service;

    /**
     * Реализация интерфейса DAO.
     *
     * @param file           - директория
     * @param flushThreshold - максимальный размер таблицы
     */
    public DAOImpl(@NotNull final File file, final long flushThreshold, final int threads) {
        this.flushThreshold = flushThreshold;
        final AtomicInteger generation = new AtomicInteger();
        this.file = file;
        final File[] list = file.listFiles((dir1, name) -> name.endsWith(SUFFIX));
        assert list != null;
        Arrays.stream(list)
                .filter(currentFile -> !currentFile.isDirectory())
                .forEach(f -> {
                            final String name = f.getName();
                            final int gen =
                                    Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            try {
                                ssTables.put(gen, new SSTable(f));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            if (gen > generation.get()) {
                                generation.set(gen);
                            }
                        }
                );
        tables = new TableSet(ssTables, generation.get());
        this.service = Executors.newFixedThreadPool(threads);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        lock.writeLock().lock();
        try {
            final Iterator<Cell> alive = Iterators.filter(compactIterator(from),
                    cell -> !requireNonNull(cell).getValue().isRemoved());
            return Iterators.transform(alive, cell ->
                    Record.of(requireNonNull(cell).getKey(), cell.getValue().getData()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean isNeedFlush;
        lock.readLock().lock();
        try {
            isNeedFlush = tables.currMemTable.size() >= flushThreshold;
            tables.currMemTable.upsert(key, value);
        } finally {
            lock.readLock().unlock();
        }
        if (isNeedFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean isNeedFlush;
        lock.readLock().lock();
        try {
            isNeedFlush = tables.currMemTable.size() >= flushThreshold;
            tables.currMemTable.remove(key);
        } finally {
            lock.readLock().unlock();
        }
        if (isNeedFlush) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (tables.currMemTable.mapSize() > 0) {
            flush();
        }
        service.shutdown();
        try {
            service.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        tables.ssTableCollection.values().forEach(Table::close);
    }

    @Override
    public void compact() throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            lock.readLock().unlock();
        }
        final ByteBuffer from = ByteBuffer.allocate(0);
        lock.writeLock().lock();
        try {
            final Collection<Iterator<Cell>> iterators = new ArrayList<>(snapshot.ssTableCollection.size());
            snapshot.ssTableCollection.descendingMap().values().forEach(table -> {
                try {
                    iterators.add(table.iterator(from));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
            final Iterator<Cell> iterator = Iters.collapseEquals(merged, Cell::getKey);
            if (iterator.hasNext()) {
                final File dest = new File(file, snapshot.generation + TEMP);
                SSTable.write(dest, iterator);
                for (int i = 0; i < snapshot.generation; i++) {
                    final File curFile = new File(file, i + SUFFIX);
                    if (curFile.exists()) {
                        Files.delete(curFile.toPath());
                    }
                }
                final File curFile = new File(file, 0 + SUFFIX);
                Files.move(dest.toPath(), curFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                tables = snapshot.compact(tables.currMemTable, new SSTable(curFile));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = tables;
            if (snapshot.currMemTable.size() == 0L) {
                return;
            }
            tables = tables.fromMemTableToFlushing(snapshot.tablesReadyToFlush);
        } finally {
            lock.writeLock().unlock();
        }
        service.execute(() -> {
            try {
                lock.writeLock().lock();
                try {
                    final File copyFile = new File(file, snapshot.generation + TEMP);
                    SSTable.write(copyFile, snapshot.currMemTable.iterator(ByteBuffer.allocate(0)));
                    final File dest = new File(file, snapshot.generation + SUFFIX);
                    Files.move(copyFile.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);

                    tables = tables.fromFlushingToSSTable(snapshot.currMemTable, tables.tablesReadyToFlush, new SSTable(dest));
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (IOException e) {
                Runtime.getRuntime().halt(-1);
            }
        });
    }

//    @NotNull
//    private Iterator<Cell> compactIterator(@NotNull final List<Iterator<Cell>> iteratorList) {
//        return Iters.collapseEquals(
//                Iterators.mergeSorted(iteratorList, Cell.COMPARATOR),
//                Cell::getKey);
//    }

    private Iterator<Cell> compactIterator(@NotNull final ByteBuffer from) throws IOException {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tables;
            final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTableCollection.size() + snapshot.tablesReadyToFlush.size() + 1);
            iters.add(snapshot.currMemTable.iterator(from));
            snapshot.ssTableCollection.descendingMap().values().forEach(table -> {
                try {
                    iters.add(table.iterator(from));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            snapshot.tablesReadyToFlush.forEach(table -> {
                try {
                    iters.add(table.iterator(from));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            final Iterator<Cell> merged = Iterators.mergeSorted(iters, Comparator.naturalOrder());
            return Iters.collapseEquals(merged, Cell::getKey);
        } finally {
            lock.readLock().unlock();
        }
    }
}
