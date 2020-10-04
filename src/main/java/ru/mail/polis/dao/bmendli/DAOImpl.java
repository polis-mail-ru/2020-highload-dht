package ru.mail.polis.dao.bmendli;

import com.google.common.collect.Iterators;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import java.util.stream.Stream;

/**
 * Persistent storage.
 *
 * @author bmendli
 */
public class DAOImpl implements DAO {

    private static final String SSTABLE_FILE_END = ".dat";
    private static final String SSTABLE_TMP_FILE_END = ".tmp";
    private static final String FILE_NAME_REGEX = "[0-9]+";
    private static final int MAX_THREADS = 4;

    private final Logger logger = LoggerFactory.getLogger(DAOImpl.class);
    @NonNull
    private final File storage;
    @NotNull
    private final MemTablePool memTablePool;
    @NotNull
    private final NavigableMap<Integer, Table> ssTables;
    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final ReadWriteLock readWriteLock;

    /**
     * Creates DAO from file storage, data from file will be store in immutable SSTable
     * and new data - MemTable.
     *
     * @param storage       - file in which store data
     * @param tableByteSize - max table byte size
     */
    public DAOImpl(final File storage,
                   final long tableByteSize,
                   final int memTablePoolSize) {
        this.storage = storage;
        this.ssTables = new ConcurrentSkipListMap<>();
        final AtomicInteger generation = new AtomicInteger(-1);

        try (Stream<Path> stream = Files.walk(storage.toPath(), 1)) {
            stream
                    .filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SSTABLE_FILE_END)
                                && !path.toFile().isDirectory()
                                && name.substring(0, name.indexOf(SSTABLE_FILE_END)).matches(FILE_NAME_REGEX);
                    })
                    .forEach(path -> {
                        try {
                            final String fileName = path.getFileName().toString();
                            final String fileGenerationStr = fileName.substring(0, fileName.indexOf(SSTABLE_FILE_END));
                            final int fileGeneration = Integer.parseInt(fileGenerationStr);
                            generation.set(Math.max(generation.get(), fileGeneration));
                            ssTables.put(fileGeneration, new SSTable(path));
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new UncheckedIOException(e);
        }

        this.readWriteLock = new ReentrantReadWriteLock();
        this.memTablePool = new MemTablePool(tableByteSize, generation.incrementAndGet(), memTablePoolSize);
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
        this.executorService.execute(() -> {
            boolean poisonReceived = false;
            while (!poisonReceived && !Thread.currentThread().isInterrupted()) {
                try {
                    final FlushTable flushTable = this.memTablePool.takeTableToFlush();
                    poisonReceived = flushTable.isPoisonPill();
                    flush(flushTable);
                    memTablePool.flushed(flushTable.getGeneration());
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators;
        readWriteLock.readLock().lock();
        try {
            iterators = listCellIterator(from);
        } finally {
            readWriteLock.readLock().unlock();
        }
        final Iterator<Cell> filteredIterator = Iterators.filter(mergeIterator(iterators),
                cell -> !cell.getValue().isTombstone() && !cell.getValue().isExpired());
        return Iterators.transform(filteredIterator, cell -> Record.of(cell.getKey(), cell.getValue().getData()));
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value,
            final long expireTime) throws IOException {
        memTablePool.upsert(key.duplicate().asReadOnlyBuffer(), value.duplicate().asReadOnlyBuffer(), expireTime);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTablePool.remove(key.duplicate().asReadOnlyBuffer());
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
            logger.error("error shut down", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void compact() throws IOException {
        final List<Iterator<Cell>> iterators = listCellIterator(ByteBuffer.allocate(0));
        final Iterator<Cell> cellIterator = mergeIterator(iterators);
        int generation = memTablePool.getGeneration();
        final List<File> oldFiles = new ArrayList<>(generation);
        readWriteLock.writeLock().lock();
        try {
            for (int i = 0; i < generation; i++) {
                final File dest = new File(storage, i + "_old" + SSTABLE_FILE_END);
                new File(storage, i + SSTABLE_FILE_END).renameTo(dest);
                oldFiles.add(dest);
            }
            generation = 0;
            final File fileTmp = new File(storage, generation + SSTABLE_TMP_FILE_END);
            SSTable.serialize(fileTmp, cellIterator);
            final File fileDst = new File(storage, generation + SSTABLE_FILE_END);
            final Path targetPath = fileDst.toPath();
            Files.move(fileTmp.toPath(), targetPath, StandardCopyOption.ATOMIC_MOVE);
            for (final File oldFile : oldFiles) {
                Files.delete(oldFile.toPath());
            }
            memTablePool.close();
            ssTables.clear();
            ssTables.put(generation, new SSTable(targetPath));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Saving data on the disk.
     */
    public void flush(@NotNull final FlushTable flushTable) throws IOException {
        final Iterator<Cell> iterator = flushTable.getTable().iterator(ByteBuffer.allocate(0));
        final File file = new File(storage, flushTable.getGeneration() + SSTABLE_TMP_FILE_END);
        SSTable.serialize(file, iterator);

        final File dst = new File(storage, flushTable.getGeneration() + SSTABLE_FILE_END);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables.put(flushTable.getGeneration(), new SSTable(dst.toPath()));
    }

    private List<Iterator<Cell>> listCellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        try {
            iterators.add(memTablePool.iterator(from));
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new UncheckedIOException(e);
        }
        ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iterators.add(ssTable.iterator(from));
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw new UncheckedIOException(e);
            }
        });

        return iterators;
    }

    @NotNull
    private Iterator<Cell> mergeIterator(List<Iterator<Cell>> iterators) {
        final Iterator<Cell> mergedCellIterator = Iterators.mergeSorted(iterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        return Iters.collapseEquals(mergedCellIterator, Cell::getKey);
    }
}
