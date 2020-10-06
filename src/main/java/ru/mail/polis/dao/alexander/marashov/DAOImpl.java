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
import java.util.NavigableMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

    private final static Logger log = LoggerFactory.getLogger(DAOImpl.class);
    private final static ReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final static String SUFFIX = ".dat";
    private final static String TEMP = ".tmp";

    private final int flusherQueueSize = 10;

    @NotNull
    private final File storage;
    private final long flushThreshold;

    @GuardedBy("lock")
    private static TableSnapshot tableSnapshot;
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
        flusher = new Flusher(storage, flusherQueueSize, this::postFlushingMethod);
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
            tableSnapshot.memTable.upsert(key.asReadOnlyBuffer().duplicate(), value.asReadOnlyBuffer().duplicate());
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
            tableSnapshot.memTable.remove(key.asReadOnlyBuffer().duplicate());
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
            System.out.println("WRITE LOCKED: flush intent to " + snapshot.memTable.size() + " with gen " + snapshot.generation);
            if (snapshot.memTable.sizeInBytes() == 0L) {
                System.out.println("WRITE LOCKED: flush intent to " + snapshot.memTable.size() + " with gen " + snapshot.generation + " CANCELED");
                return;
            }

            tableSnapshot = snapshot.flushIntent();
        } finally {
            System.out.println("WRITE LOCK UNLOCKED");
            lock.writeLock().unlock();
        }
        try {
            System.out.println("TABLE QUEUE PUT: " + snapshot.memTable.size() + " with gen " + snapshot.generation);
            flusher.tablesQueue.put(new NumberedTable(snapshot.memTable, snapshot.generation));
            System.out.println("TABLE QUEUE PUT: " + snapshot.memTable.size() + " with gen " + snapshot.generation + " ENDED");
        } catch (InterruptedException e) {
            log.info("Flush waiting interrupted", e);
        }
    }

    public static class Flusher extends Thread {
        final File storage;
        final BlockingQueue<NumberedTable> tablesQueue;
        final BiConsumer<Integer, File> tableFlashedCallback;

        public Flusher(
                final File storage,
                final int initialCapacity,
                final BiConsumer<Integer, File> tableFlushedCallback
        ) {
            this.storage = storage;
            this.tablesQueue = new ArrayBlockingQueue<>(initialCapacity);
            this.tableFlashedCallback = tableFlushedCallback;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    final NumberedTable numberedTable = tablesQueue.take();
                    if (numberedTable.table == null) {
                        log.info("Flusher stopped");
                        return;
                    }
                    log.info(String.format("Flushing table %d, gen %d", numberedTable.table.size(), numberedTable.generation));
                    final int generation = numberedTable.generation;
                    final Iterator<Cell> cellIterator = numberedTable.table.iterator(ByteBuffer.allocate(0));
                    final File file = new File(this.storage, generation + TEMP);
                    SSTable.serialize(cellIterator, file);
                    final File dst = new File(this.storage, generation + SUFFIX);
                    Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
//                    this.tableFlashedCallback.accept(numberedTable.generation, dst);
                    log.info(String.format("FlushED table %d, gen %d", numberedTable.table.size(), numberedTable.generation));
                    lock.writeLock().lock();
                    try {
                        tableSnapshot = tableSnapshot.loadTableIntent(generation, dst);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } catch (final InterruptedException e) {
                log.error("Flusher interrupted. The program stops.", e);
            } catch (final IOException e) {
                log.error("Flusher met an unexpected IOException. The program stops.", e);
            }
            System.exit(-1);
        }
    }

    private File writeDataToFile(final Iterator<Cell> cellIterator, final Integer generation) throws IOException {
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(cellIterator, file);
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        return dst;
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
        } catch (InterruptedException e) {
            log.info("Stopping interrupted", e);
        }
    }

    @Override
    public void compact() throws IOException {
        final List<TableIterator> tableIteratorList;
        final int lastGen;
        int index = 0;
        lock.readLock().lock();
        try {
            lastGen = tableSnapshot.generation;
            final int tableIteratorsCount = tableSnapshot.storageTables.size() + 1;
            tableIteratorList = new ArrayList<>(
                    tableIteratorsCount
            );
            for (final Table table : tableSnapshot.storageTables.values()) {
                tableIteratorList.add(
                        new TableIterator(index, table)
                );
                ++index;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            tableIteratorList.add(new TableIterator(tableSnapshot.generation, tableSnapshot.memTable));
            tableSnapshot = tableSnapshot.flushIntent();
        } finally {
            lock.writeLock().unlock();
        }

        final Iterator<Cell> cellIterator = new CellIterator(tableIteratorList);
        final File file = writeDataToFile(cellIterator, lastGen);

        lock.writeLock().lock();
        try {
            tableSnapshot = tableSnapshot.compactIntent(lastGen, file);
        } finally {
            lock.writeLock().unlock();
        }
        doWithFiles(storage.toPath(), (gen, path) -> {
            if (gen < lastGen) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private void doWithFiles(final Path storagePath, final BiConsumer<Integer, Path> genBiConsumer) {
        try (Stream<Path> stream = Files.list(storagePath)) {
            stream.filter(p -> p.toString().endsWith(SUFFIX))
                    .forEach(path -> {
                        final String name = path.getFileName().toString();
                        final String genStr = name.substring(0, name.indexOf(SUFFIX));
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
