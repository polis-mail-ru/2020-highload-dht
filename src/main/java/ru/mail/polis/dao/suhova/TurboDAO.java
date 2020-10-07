package ru.mail.polis.dao.suhova;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.Record;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNull;

public class TurboDAO implements DAO {
    private static final String SUFFIX = "sst.dat";
    private static final String TEMP = "sst.tmp";
    private final long flushThreshold;
    private final File dir;
    private TableSet tables;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    @NotNull
    private final ExecutorService service;

    /**
     * Implementation {@link DAO}.
     *
     * @param dir            - directory
     * @param flushThreshold - when the table reaches this size, it flushes
     */
    public TurboDAO(@NotNull final File dir, final long flushThreshold) {
        this.flushThreshold = flushThreshold;
        this.dir = dir;
        final AtomicInteger generation = new AtomicInteger();
        final NavigableMap<Integer, Table> ssTables = new ConcurrentSkipListMap<>();
        final File[] list = dir.listFiles((dir1, name) -> name.endsWith(SUFFIX));
        assert list != null;
        Arrays.stream(list)
            .filter(file -> !file.isDirectory())
            .forEach(
                f -> {
                    final String name = f.getName();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    try {
                        ssTables.put(gen, new SSTable(f));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    if (gen > generation.get()) generation.set(gen);
                }
            );
        tables = new TableSet(ssTables, generation.get());
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        lock.writeLock().lock();
        try {
            final Iterator<Cell> alive = Iterators.filter(cellIterator(from),
                cell -> !requireNonNull(cell).getValue().isTombstone());
            return Iterators.transform(alive, cell ->
                Record.of(requireNonNull(cell).getKey(), cell.getValue().getData()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        lock.readLock().lock();
        try {
            snapshot = this.tables;
            final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size() + snapshot.flushing.size() + 1);
            iters.add(snapshot.memTable.iterator(from));
            snapshot.ssTables.descendingMap().values().forEach(table -> iters.add(table.iterator(from)));
            snapshot.flushing.forEach(table -> iters.add(table.iterator(from)));
            final Iterator<Cell> merged = Iterators.mergeSorted(iters, Comparator.naturalOrder());
            return Iters.collapseEquals(merged, Cell::getKey);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final boolean needsFlush;
        lock.readLock().lock();
        try {
            needsFlush = tables.memTable.sizeInBytes() >= flushThreshold;
            tables.memTable.upsert(key, value);
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
            needsFlush = tables.memTable.sizeInBytes() >= flushThreshold;
            tables.memTable.remove(key);
        } finally {
            lock.readLock().unlock();
        }
        if (needsFlush) {
            flush();
        }
    }

    @Override
    public void close() {
        if (tables.memTable.getEntryCount() > 0) {
            flush();
        }
        service.shutdown();
        try {
            service.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getCause());
        }
        tables.ssTables.values().forEach(Table::close);
    }

    private void flush() {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = tables;
            if (snapshot.memTable.sizeInBytes() == 0L) {
                return;
            }
            tables = tables.fromMemTableToFlushing(snapshot.flushing);
        } finally {
            lock.writeLock().unlock();
        }
        service.execute(() -> {
            try {
                lock.writeLock().lock();
                try {
                    final File tmp = new File(dir, snapshot.generation + TEMP);
                    SSTable.write(tmp, snapshot.memTable.iterator(ByteBuffer.allocate(0)));
                    final File dat = new File(dir, snapshot.generation + SUFFIX);
                    Files.move(tmp.toPath(), dat.toPath(), StandardCopyOption.ATOMIC_MOVE);

                    tables = tables.fromFlushingToSSTable(snapshot.memTable, tables.flushing, new SSTable(dat));
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (IOException e) {
                Runtime.getRuntime().halt(-1);
            }
        });
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSet snapshot;
        lock.writeLock().lock();
        try {
            snapshot = this.tables;

            final ByteBuffer from = ByteBuffer.allocate(0);
            final Collection<Iterator<Cell>> iterators = new ArrayList<>(snapshot.ssTables.size() + 1);
            iterators.add(snapshot.memTable.iterator(from));
            snapshot.ssTables.descendingMap().values().forEach(table -> iterators.add(table.iterator(from)));
            final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());

            final Iterator<Cell> iterator = Iters.collapseEquals(merged, Cell::getKey);
            if (iterator.hasNext()) {
                final File tmp = new File(dir, snapshot.generation + TEMP);
                SSTable.write(tmp, iterator);
                for (int i = 0; i < snapshot.generation; i++) {
                    final File file = new File(dir, i + SUFFIX);
                    if (file.exists()) {
                        Files.delete(file.toPath());
                    }
                }
                final File file = new File(dir, 0 + SUFFIX);
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
                tables = snapshot.compact(new SSTable(file));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
