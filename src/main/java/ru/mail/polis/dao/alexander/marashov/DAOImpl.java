package ru.mail.polis.dao.alexander.marashov;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Persistent storage.
 *
 * @author Alexander Marashov
 */
public class DAOImpl implements DAO {

    public static final String SUFFIX = ".dat";
    public static final String TEMP = ".tmp";
    public static final String UNDERSCORE = "_";

    public static final Logger log = LoggerFactory.getLogger(DAOImpl.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock(false);

    @GuardedBy("lock")
    private TableSnapshot tableSnapshot;
    private final long flushThreshold;
    private final long tablesToCompactCount;
    private final Flusher flusher;
    private final Compactor compactor;

    /**
     * Creates DAO from storage file with flushThreshold data limit.
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold, final long tablesToCompactCount) {
        assert flushThreshold > 0L;
        this.flushThreshold = flushThreshold;
        this.tablesToCompactCount = tablesToCompactCount;

        final NavigableMap<Integer, Table> ssTables = new ConcurrentSkipListMap<>();
        final NavigableMap<Integer, Table> flushingTables = new ConcurrentSkipListMap<>();

        final AtomicInteger maxGeneration = new AtomicInteger();
        doWithFiles(storage.toPath(), (gen, path) -> {
            ssTables.put(gen, new SSTable(path.toFile()));
            maxGeneration.set(Math.max(maxGeneration.get(), gen));
        });
        tableSnapshot = TableSnapshot.initializeTables(
                ssTables,
                flushingTables,
                maxGeneration.get() + 1
        );
        flusher = new Flusher(storage, this::postFlushingMethod);
        flusher.start();
        compactor = new Compactor(
                storage,
                this::getStorageTablesSnapshot,
                tables -> maxGen -> dst -> postCompactingMethod(tables, maxGen, dst)
        );
        compactor.start();
    }

    private void postFlushingMethod(final Integer generation, final File file) {
        final TableSnapshot snapshot;
        lock.writeLock().lock();
        try {
            tableSnapshot = tableSnapshot.loadTableIntent(generation, file);
            snapshot = tableSnapshot;
        } finally {
            lock.writeLock().unlock();
        }

        // we won't wait
        if (snapshot.storageTables.size() >= tablesToCompactCount) {
            compact();
        }
    }

    private NavigableMap<Integer, Table> getStorageTablesSnapshot() {
        lock.readLock().lock();
        try {
            return tableSnapshot.storageTables;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void postCompactingMethod(
            final NavigableMap<Integer, Table> tablesToCompact,
            final Integer maxGeneration,
            final File dst
    ) {
        lock.writeLock().lock();
        try {
            tableSnapshot = tableSnapshot.compactIntent(tablesToCompact, maxGeneration, dst);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final Iterator<Cell> fresh = cellIterator(from);
        final Iterator<Cell> alive = Iterators.filter(fresh, i -> !i.getValue().isTombstone());
        return Iterators.transform(alive, i -> Record.of(i.getKey(), i.getValue().getData()));
    }

    @NotNull
    @Override
    public Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) throws IOException {
        final TableSnapshot snapshot;
        lock.readLock().lock();
        try {
            snapshot = tableSnapshot;
        } finally {
            lock.readLock().unlock();
        }
        final int iteratorsCount = snapshot.storageTables.size() + snapshot.flushingTables.size() + 1;
        final List<Iterator<Cell>> iterators = new ArrayList<>(iteratorsCount);
        iterators.add(snapshot.memTable.iterator(from));
        for (final Table t : snapshot.flushingTables.descendingMap().values()) {
            iterators.add(t.iterator(from));
        }
        for (final Table t : snapshot.storageTables.descendingMap().values()) {
            iterators.add(t.iterator(from));
        }
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell.COMPARATOR);
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        upsert(key, value, Value.NEVER_EXPIRES);
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value,
            final long expiresTimestamp
    ) throws IOException {
        final boolean needFlush;
        lock.readLock().lock();
        try {
            tableSnapshot.memTable.upsert(key.duplicate(), value.duplicate(), expiresTimestamp);
            needFlush = tableSnapshot.memTable.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }
        if (needFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean needFlush;
        lock.readLock().lock();
        try {
            tableSnapshot.memTable.remove(key.duplicate());
            needFlush = tableSnapshot.memTable.sizeInBytes() > flushThreshold;
        } finally {
            lock.readLock().unlock();
        }
        if (needFlush) {
            flush();
        }
    }

    /**
     * Saving data on the disk.
     */
    public void flush() throws IOException {
        final TableSnapshot snapshot;
        lock.writeLock().lock();
        try {
            snapshot = tableSnapshot;
            if (snapshot.memTable.sizeInBytes() == 0L) {
                return;
            }
            tableSnapshot = snapshot.flushIntent();
        } finally {
            lock.writeLock().unlock();
        }
        try {
            flusher.tablesQueue.put(new NumberedTable(snapshot.memTable, snapshot.generation));
        } catch (final InterruptedException e) {
            log.error("Flush waiting interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void compact() {
        try {
            compactor.tasksQueue.add(new CompactorTask(false));
        } catch (final IllegalStateException e) {
            log.info("Compactor queue is full!", e);
        }
    }

    @Override
    public void close() throws IOException {
        TableSnapshot snapshot;
        lock.readLock().lock();
        try {
            snapshot = tableSnapshot;
        } finally {
            lock.readLock().unlock();
        }

        if (snapshot.memTable.size() > 0) {
            flush();
        }
        try {
            flusher.tablesQueue.put(new NumberedTable(null, -1));
            compactor.tasksQueue.put(new CompactorTask(true));
            flusher.join();
            compactor.join();
        } catch (final InterruptedException e) {
            log.error("Stopping interrupted", e);
            Thread.currentThread().interrupt();
        }

        lock.readLock().lock();
        try {
            snapshot = tableSnapshot;
        } finally {
            lock.readLock().unlock();
        }

        snapshot.storageTables.forEach((i, t) -> {
            try {
                t.close();
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
        });
    }

    private void doWithFiles(final Path storagePath, final BiConsumer<Integer, Path> genBiConsumer) {
        try (Stream<Path> stream = Files.list(storagePath)) {
            stream.filter(p -> p.toString().endsWith(SUFFIX))
                    .forEach(path -> {
                        final String name = path.getFileName().toString();
                        final String rowStr = name.substring(0, name.indexOf(SUFFIX));
                        final int underscoreInd = rowStr.indexOf('_');
                        final String genStr = underscoreInd == -1 ? rowStr : rowStr.substring(0, underscoreInd);
                        if (genStr.matches("[0-9]+")) {
                            final int gen = Integer.parseInt(genStr);
                            genBiConsumer.accept(gen, path);
                        }
                    });
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }
}
