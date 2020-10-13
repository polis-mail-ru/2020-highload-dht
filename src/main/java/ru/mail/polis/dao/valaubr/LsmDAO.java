package ru.mail.polis.dao.valaubr;

import com.google.common.collect.Iterators;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * DAO implementation.
 * Persistence storage.
 *
 * @author Ilya Chuprynin
 */
public class LsmDAO implements DAO {

    private static final String FILE_POSTFIX = ".dat";
    private static final String TEMP_FILE_POSTFIX = ".tmp";
    private static final String LSM_TEMP_FILE = "temp.tmp";
    private static final int POOL_SIZE = 2;
    private static final int NUMBER_OF_THREADS = 4;

    @NotNull
    private final File storage;
    private final NavigableMap<Integer, Table> ssTables;
    private final Pattern pattern = Pattern.compile("^\\d+$");
    private final TablesPool memTablePool;
    private final ExecutorService executorService;
    private final ReadWriteLock readWriteLock;
    private final Logger log = LoggerFactory.getLogger(LsmDAO.class);
    private final AtomicInteger generation = new AtomicInteger(0);

    /**
     * DAO constructor for storage file with size limit.
     *
     * @param storage        the path to the file where the files will be stored
     * @param flushThreshold max table size
     * @throws IOException file work exception
     */
    public LsmDAO(
            @NotNull final File storage,
            final long flushThreshold) throws IOException {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.ssTables = new ConcurrentSkipListMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(file -> !file.toFile().isDirectory() && file.toString().endsWith(FILE_POSTFIX))
                    .forEach(file -> {
                        final String fileName = file.getFileName().toString();
                        try {
                            if (pattern.matcher(fileName.substring(0, fileName.indexOf(FILE_POSTFIX))).find()) {
                                final int gen = Integer.parseInt(fileName.substring(0, fileName.indexOf(FILE_POSTFIX)));
                                if (gen > generation.get()) {
                                    generation.set(gen);
                                }
                                ssTables.put(gen, new SSTable(file.toFile()));
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            generation.addAndGet(1);
            this.readWriteLock = new ReentrantReadWriteLock();
            this.memTablePool = new TablesPool(flushThreshold, generation.addAndGet(1), POOL_SIZE);
            this.executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            this.executorService.execute(this::flushingHelper);
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        readWriteLock.readLock().lock();
        try {
            final Iterator<Cell> alive = Iterators.filter(cellIterator(from),
                    cell -> !requireNonNull(cell).getValue().isTombstone());
            return Iterators.transform(alive, cell ->
                    Record.of(requireNonNull(cell).getKey(),
                            cell.getValue().getData()));
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iters = new ArrayList<>(ssTables.size() + 1);
        try {
            iters.add(memTablePool.iterator(from));
        } catch (IOException e) {
            log.error("Bad iterator from memTablePool:", e);
            throw new UncheckedIOException(e);
        }
        ssTables.descendingMap().values().forEach(table -> {
            try {
                iters.add(table.iterator(from));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);

        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
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
            Thread.currentThread().interrupt();
        }
    }

    private void flushingHelper() {
        boolean poisonReceived = false;
        while (!poisonReceived && !Thread.currentThread().isInterrupted()) {
            FlushingTable flushingTable;
            try {
                flushingTable = this.memTablePool.takeToFlash();
                poisonReceived = flushingTable.isPoisonPill();
                flush(flushingTable);
                memTablePool.flushed(flushingTable.getGen());
            } catch (InterruptedException e) {
                log.error("Interrupt while creating table", e);
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("Error while creating table", e);
            }
        }
    }

    private void flush(FlushingTable flushingTable) throws IOException {
        readWriteLock.writeLock().lock();
        try {
            final File file = new File(storage, generation + TEMP_FILE_POSTFIX);
            file.createNewFile();
            SSTable.serialize(file, flushingTable.getTable().iterator(ByteBuffer.allocate(0)));
            final File dst = new File(storage, generation + FILE_POSTFIX);
            Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
            generation.addAndGet(1);
            ssTables.put(generation.get(), new SSTable(dst));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void compact() throws IOException {
        readWriteLock.writeLock().lock();
        try {
            final File tempFile = new File(storage, LSM_TEMP_FILE);
            tempFile.createNewFile();
            SSTable.serialize(
                    tempFile,
                    requireNonNull(cellIterator(ByteBuffer.allocate(0)))
            );
            for (int i = 1; i < generation.get(); i++) {
                final File deletingFile = new File(storage, i + FILE_POSTFIX);
                if (deletingFile.exists()) {
                    Files.delete(deletingFile.toPath());
                }
            }
            generation.set(1);
            final File dst = new File(storage, 1 + FILE_POSTFIX);
            dst.createNewFile();
            Files.move(tempFile.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
            ssTables.clear();
            ssTables.put(generation.addAndGet(1), new SSTable(dst));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}

