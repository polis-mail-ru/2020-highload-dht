package ru.mail.polis.dao.impl;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Cell;
import ru.mail.polis.MemTable;
import ru.mail.polis.Record;
import ru.mail.polis.SSTable;
import ru.mail.polis.Table;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.TableSet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNull;

public final class DAOImpl implements DAO {

    private static final String SUFFIX = "SSTable.dat";
    private static final String TEMP = "SSTable.tmp";
    private final long flushThreshold;
    private final File file;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private TableSet memTable;

    /**
     * Реализация интерфейса DAO.
     *
     * @param file           - директория
     * @param flushThreshold - максимальный размер таблицы
     */
    public DAOImpl(@NotNull final File file, final long flushThreshold) {
        this.flushThreshold = flushThreshold;
        this.file = file;
        final AtomicInteger generation = new AtomicInteger();
        final NavigableMap<Integer, Table> ssTables = new ConcurrentSkipListMap<>();
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
        memTable = new TableSet(ssTables, generation.get());
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        lock.readLock().lock();
        try {
            final Iterator<Cell> alive = Iterators.filter(
                    compactIterator(from),
                    cell -> !requireNonNull(cell).getValue().isRemoved());
            return Iterators.transform(alive, cell ->
                    Record.of(requireNonNull(cell).getKey(), cell.getValue().getData()));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final boolean needsFlush;
        lock.readLock().lock();
        try {
            needsFlush = memTable.memTable.size() >= flushThreshold;
            memTable.memTable.upsert(key, value);
        } finally {
            lock.readLock().unlock();
        }
        if (needsFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final boolean needsFlush;
        lock.readLock().lock();
        try {
            needsFlush = memTable.memTable.size() >= flushThreshold;
            memTable.memTable.remove(key);
        } finally {
            lock.readLock().unlock();
        }
        if (needsFlush) {
            flush();
        }
    }

    @Override
    public void close() {
        if (memTable.memTable.getMapSize() > 0) {
            flush();
        }
        memTable.ssTable.values().forEach(Table::close);
    }

    @Override
    public void compact() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.memTable;
        } finally {
            lock.writeLock().unlock();
        }
        final ByteBuffer from = ByteBuffer.allocate(0);
        final Collection<Iterator<Cell>> iterators = new ArrayList<>(snapshot.ssTable.size() + 1);
        iterators.add(snapshot.memTable.iterator(from));
        snapshot.ssTable.descendingMap().values().forEach(table -> iterators.add(table.iterator(from)));
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
        final Iterator<Cell> iterator = Iters.collapseEquals(merged, Cell::getKey);
        if (iterator.hasNext()) {
            final File file = new File(this.file, memTable.generation + TEMP);
            SSTable.write(file, iterator);
            for (int i = 0; i < snapshot.generation; i++) {
                final File dir = new File(this.file, i + SUFFIX);
                if (dir.exists()) {
                    Files.delete(dir.toPath());
                }
            }
            snapshot.generation = 0;
            final File dir = new File(this.file, snapshot.generation + SUFFIX);
            Files.move(file.toPath(), dir.toPath(), StandardCopyOption.ATOMIC_MOVE);
            snapshot.ssTable.clear();
            snapshot.ssTable.put(snapshot.generation, new SSTable(dir));
            memTable = new TableSet(new MemTable(), snapshot.readyToFlush, snapshot.ssTable, snapshot.generation + 1);
        }
    }

    private void flush() {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = memTable;
            if (snapshot.memTable.size() == 0L) {
                return;
            }
            memTable = memTable.addToFlush();
        } finally {
            lock.writeLock().unlock();
        }
        try {
            final File file = new File(this.file, snapshot.generation + TEMP);
            SSTable.write(file, snapshot.memTable.iterator(ByteBuffer.allocate(0)));
            final File dir = new File(this.file, snapshot.generation + SUFFIX);
            Files.move(file.toPath(), dir.toPath(), StandardCopyOption.ATOMIC_MOVE);
            lock.writeLock().lock();
            try {
                memTable = memTable.flush(snapshot.memTable, new SSTable(dir), snapshot.generation);
            } finally {
                lock.writeLock().unlock();
            }
        } catch (IOException e) {
            Runtime.getRuntime().halt(-1);
        }
    }

    @NotNull
    private Iterator<Cell> compactIterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.memTable;
            final List<Iterator<Cell>> iterators = new ArrayList<>(snapshot.ssTable.size() + snapshot.readyToFlush
                    .size() + 1);
            iterators.add(snapshot.memTable.iterator(from));
            snapshot.ssTable.descendingMap().values().forEach(table -> iterators.add(table.iterator(from)));
            snapshot.readyToFlush.forEach(table -> iterators.add(table.iterator(from)));
            final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
            return Iters.collapseEquals(merged, Cell::getKey);
        } finally {
            lock.readLock().unlock();
        }
    }
}
