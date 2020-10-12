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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    static final String SUFFIX = ".dat";
    static final String TEMP = ".tmp";
    static final String UNDERSCORE = "_";

    private final Logger log = LoggerFactory.getLogger(DAOImpl.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock(false);

    @GuardedBy("lock")
    private TableSnapshot tableSnapshot;
    @NotNull
    private final File storage;
    private final long flushThreshold;
    private final Flusher flusher;

    /**
     * Creates DAO from storage file with flushThreshold data limit.
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold) {
        assert flushThreshold > 0L;
        this.flushThreshold = flushThreshold;
        this.storage = storage;

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
    }

    private void postFlushingMethod(final Integer generation, final File file) {
        lock.writeLock().lock();
        try {
            tableSnapshot = tableSnapshot.loadTableIntent(generation, file);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
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
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        final Iterator<Cell> alive = Iterators.filter(fresh, i -> !i.getValue().isTombstone());
        return Iterators.transform(alive, i -> Record.of(i.getKey(), i.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean needFlush;
        lock.readLock().lock();
        try {
            tableSnapshot.memTable.upsert(key.duplicate(), value.duplicate());
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
    public void close() throws IOException {
        final TableSnapshot snapshot;
        lock.readLock().lock();
        try {
            snapshot = tableSnapshot;
        } finally {
            lock.readLock().unlock();
        }

        if (snapshot.memTable.size() > 0) {
            flush();
        }
        snapshot.storageTables.forEach((i, t) -> {
            try {
                t.close();
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
        });
        try {
            flusher.tablesQueue.put(new NumberedTable(null, -1));
            flusher.join();
        } catch (final InterruptedException e) {
            log.error("Stopping interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSnapshot snapshot;
        lock.readLock().lock();
        try {
            if (tableSnapshot.storageTables.size() <= 1) { // nothing to compact
                return;
            }
            snapshot = tableSnapshot;
        } finally {
            lock.readLock().unlock();
        }

        final int tableIteratorsCount = snapshot.storageTables.size();
        int maxGeneration = 0;
        final List<TableIterator> tableIteratorList = new ArrayList<>(tableIteratorsCount);
        for (final Map.Entry<Integer, Table> entry : snapshot.storageTables.entrySet()) {
            maxGeneration = Math.max(maxGeneration, entry.getKey());
            tableIteratorList.add(new TableIterator(entry.getKey(), entry.getValue()));
        }
        final Iterator<Cell> cellIterator = new CellIterator(tableIteratorList);
        final File file = new File(this.storage, maxGeneration + UNDERSCORE + TEMP);
        SSTable.serialize(cellIterator, file);
        final File dst = new File(this.storage, maxGeneration + UNDERSCORE + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            tableSnapshot = tableSnapshot.compactIntent(snapshot.storageTables, maxGeneration, dst);
        } finally {
            lock.writeLock().unlock();
        }
        for (final Table table : snapshot.storageTables.values()) {
            final Path path = table.getFile().toPath();
            try {
                Files.delete(path);
            } catch (final IOException e) {
                throw new IOException("Flusher: can't delete file " + path, e);
            }
        }
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
