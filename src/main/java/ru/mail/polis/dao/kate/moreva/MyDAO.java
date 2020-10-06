package ru.mail.polis.dao.kate.moreva;

import com.google.common.collect.Iterators;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Dao implementation.
 *
 * @author kate
 */
public class MyDAO implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TMP = ".tmp";
    private static final String LETTERS = "[a-zA-Z]+";

    private final Logger log = LoggerFactory.getLogger(MyDAO.class);
    @NonNull
    private final File storage;
    @NotNull
    private final TablesPool memTablePool;
    @NotNull
    private final NavigableMap<Integer, Table> ssTables;
    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final ReadWriteLock readWriteLock;

    private int generation;

    /**
     * Creates DAO from file storage.
     *
     * @param storage        - file
     * @param flushThreshold - table size
     */
    public MyDAO(final File storage, final long flushThreshold) {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.ssTables = new ConcurrentSkipListMap<>();
        this.generation = -1;

        try (Stream<Path> stream = Files.walk(storage.toPath(), 1)) {
            stream.filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SUFFIX)
                                && !name.substring(0, name.indexOf(SUFFIX)).matches(LETTERS)
                                && !path.toFile().isDirectory();
                    })
                    .forEach(this::storeData);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new UncheckedIOException(e);
        }
        generation++;

        this.readWriteLock = new ReentrantReadWriteLock();
        this.memTablePool = new TablesPool(flushThreshold, generation, 2);
        this.executorService = Executors.newFixedThreadPool(2);
        this.executorService.execute(() -> {
            boolean poisonReceived = false;
            while (!poisonReceived && !Thread.currentThread().isInterrupted()) {
                FlushingTable flushTable;
                try {
                    flushTable = this.memTablePool.takeToFlash();
                    poisonReceived = flushTable.isPoisonPill();
                    flush(flushTable);
                    memTablePool.flushed(flushTable.getGeneration());
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private void storeData(@NotNull final Path path) {
        try {
            final String fileName = path.getFileName().toString();
            final int generationCounter = Integer.parseInt(fileName.substring(0, fileName.indexOf(SUFFIX)));
            generation = Math.max(generation, generationCounter);
            ssTables.put(generationCounter, new SSTable(path));
        } catch (NumberFormatException e) {
            log.error("Wrong name", e);
        }
    }

    /**
     *
     */
    public void flush(@NotNull final FlushingTable flushTable) throws IOException {
        readWriteLock.writeLock().lock();
        try {
            final Iterator<Cell> iterator = flushTable.getTable().iterator(ByteBuffer.allocate(0));
            if (iterator.hasNext()) {
                final File file = new File(storage, flushTable.getGeneration() + TMP);
                SSTable.serialize(file, iterator);

                final File dst = new File(storage, flushTable.getGeneration() + SUFFIX);
                Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

                ssTables.put(flushTable.getGeneration(), new SSTable(dst.toPath()));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private List<Iterator<Cell>> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        try {
            iterators.add(memTablePool.iterator(from));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new UncheckedIOException(e);
        }
        ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iterators.add(ssTable.iterator(from));
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new UncheckedIOException(e);
            }
        });
        return iterators;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators;
        readWriteLock.readLock().lock();
        try {
            iterators = cellIterator(from);

            final Iterator<Cell> mergedCellIterator = Iterators.mergeSorted(iterators,
                    Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));

            final Iterator<Cell> lastCellIterator = Iters.collapseEquals(mergedCellIterator, Cell::getKey);

            final Iterator<Cell> filteredIterator = Iterators.filter(lastCellIterator,
                    cell -> {
                        assert cell != null;
                        return !cell.getValue().isTombstone();
                    });

            return Iterators.transform(filteredIterator,
                    cell -> Record.of(Objects.requireNonNull(cell).getKey(), cell.getValue().getData()));
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value) {
        memTablePool.upsert(key, value.asReadOnlyBuffer());
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        memTablePool.remove(key);
    }

    @Override
    public void close() {
        memTablePool.close();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("error shut down", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void compact() throws IOException {
        readWriteLock.writeLock().lock();
        try {
            final List<Iterator<Cell>> iterators = cellIterator(ByteBuffer.allocate(0));
            final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
            final Iterator<Cell> iterator = Iters.collapseEquals(merged, Cell::getKey);
            final File tmpFile = new File(storage, generation + TMP);
            SSTable.serialize(tmpFile, iterator);
            for (int i = 0; i < generation; i++) {
                Files.delete(new File(storage, i + SUFFIX).toPath());
            }
            generation = 0;
            final File datFile = new File(storage, generation + SUFFIX);
            Files.move(tmpFile.toPath(), datFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            ssTables.clear();
            ssTables.put(generation, new SSTable(datFile.toPath()));
            generation++;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
