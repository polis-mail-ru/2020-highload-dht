package ru.mail.polis.dao.zvladn7;

import com.google.common.collect.Iterators;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LsmDAOImpl implements LsmDAO {

    private static final Logger logger = LoggerFactory.getLogger(LsmDAOImpl.class);

    private static final String SSTABLE_FILE_POSTFIX = ".dat";
    private static final String SSTABLE_TEMPORARY_FILE_POSTFIX = ".tmp";

    private static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    @NonNull
    private final File storage;
    private final int amountOfBytesToFlush;
    Map<ByteBuffer, Long> lockTable = new HashMap<>();

    private int generation;
    TableSet tableSet;


    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    @NotNull
    private final BlockingQueue<FlushTask> tasks;
    @NotNull
    private final Thread flusher;

    private static final class FlushTask {

        final MemoryTable memTable;
        final int generation;

        public FlushTask(MemoryTable memTable, int generation) {
            this.memTable = memTable;
            this.generation = generation;
        }

    }

    private static final FlushTask POISON_PILL = new FlushTask(null, -1);

    /**
     * LSM DAO implementation.
     * @param storage - the directory where SSTables stored.
     * @param amountOfBytesToFlush - amount of bytes that need to flush current memory table.
     */
    public LsmDAOImpl(
            @NotNull final File storage,
            final int amountOfBytesToFlush,
            final int flushQueueSize) throws IOException {
        this.storage = storage;
        this.amountOfBytesToFlush = amountOfBytesToFlush;
        NavigableMap<Integer, Table> ssTables = new TreeMap<>();
        generation = 0;
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(file -> !file.toFile().isDirectory() && file.toString().endsWith(SSTABLE_FILE_POSTFIX))
                    .forEach(file -> {
                        final String fileName = file.getFileName().toString();
                        try {
                            final String stringGen = fileName.substring(0, fileName.indexOf(SSTABLE_FILE_POSTFIX));
                            final int gen = Integer.parseInt(stringGen);
                            generation = Math.max(gen, generation);
                            ssTables.put(gen, new SSTable(file.toFile()));
                        } catch (IOException e) {
                            e.printStackTrace();
                            logger.error("Something went wrong while the SSTable was created!", e);
                        } catch (NumberFormatException e) {
                            logger.info("Unexpected name of SSTable file: " + fileName, e);
                        }
                    });
            ++generation;
        }

        this.tableSet = TableSet.provideTableSet(ssTables, generation);
        this.tasks = new ArrayBlockingQueue<>(flushQueueSize);
        this.flusher = new Thread(provideFlusherRunnable());
        flusher.start();
    }

    private Runnable provideFlusherRunnable() {
        return () -> {
            try {
                while (true) {
                    final FlushTask task = tasks.take();
                    if (task.memTable == null) {
                        break;
                    }
                    final File dst = serialize(task.generation, task.memTable.iterator(EMPTY_BUFFER));
                    writeLock.lock();
                    try {
                        tableSet = tableSet.finishFlushingOnDisk(task.memTable, dst, task.generation);
                    } finally {
                        writeLock.unlock();
                    }
                }
            } catch (InterruptedException | IOException e) {
                logger.error("Cannot flush memory table on disk", e);
                Runtime.getRuntime().halt(-1);
            }
        };
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        readLock.lock();
        try {
            snapshot = this.tableSet;
        } finally {
            readLock.unlock();
        }
        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size() + snapshot.memToFlush.size() + 2);
        iters.add(snapshot.memTable.iterator(from));
        snapshot.memToFlush.forEach(mem -> iters.add(mem.iterator(from)));
        final Iterator<Cell> freshElements = freshCellIterator(from, iters, snapshot);
        final Iterator<Cell> aliveElements = Iterators.filter(freshElements, el -> !el.getValue().isTombstone());

        return Iterators.transform(aliveElements, el -> Record.of(el.getKey(), el.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        boolean isReadyToFlush;
        readLock.lock();
        try {
            tableSet.memTable.upsert(key, value);
            isReadyToFlush = tableSet.memTable.getAmountOfBytes() > amountOfBytesToFlush;
        } finally {
            readLock.unlock();
        }
        if (isReadyToFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        boolean isReadyToFlush;
        readLock.lock();
        try {
            tableSet.memTable.remove(key);
            isReadyToFlush = tableSet.memTable.getAmountOfBytes() > amountOfBytesToFlush;
        } finally {
            readLock.unlock();
        }
        if (isReadyToFlush) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        boolean isReadyToFlush;
        readLock.lock();
        try {
            isReadyToFlush = tableSet.memTable.size() > 0;
        } finally {
            readLock.unlock();
        }
        if (isReadyToFlush) {
            flush();
            try {
                tasks.put(POISON_PILL);
                flusher.join();
            } catch (InterruptedException e) {
                logger.error("Unable to stop flusher thread on dao close.", e);
                throw new RuntimeException("Cannot stop flusher", e);
            }
        }
        tableSet.ssTables.values().forEach(Table::close);
    }

    @Override
    public synchronized void compact() throws IOException {
        boolean isEmptyListOfFiles;
        readLock.lock();
        try {
            isEmptyListOfFiles = tableSet.ssTables.isEmpty();
        } finally {
            readLock.unlock();
        }
        if (isEmptyListOfFiles) {
            return;
        }
        final TableSet snapshot;
        writeLock.lock();
        try {
            snapshot = tableSet;
            tableSet = tableSet.startCompact();
        } finally {
            writeLock.unlock();
        }

        final List<Iterator<Cell>> iters = new ArrayList<>(snapshot.ssTables.size());
        final Iterator<Cell> freshElements = freshCellIterator(EMPTY_BUFFER, iters, snapshot);
        final File dst = serialize(snapshot.generation, freshElements);

        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(f -> !f.getFileName().toFile().toString().equals(dst.getName()))
                .forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        logger.warn("Unable to delete file: " + f.getFileName().toFile().toString(), e);
                    }
                });
        }

        writeLock.lock();
        try {
            tableSet = tableSet.finishCompact(snapshot.ssTables, dst, snapshot.generation);
        } finally {
            writeLock.unlock();
        }
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        writeLock.lock();
        try {
            snapshot = tableSet;
            if (snapshot.memTable.size() == 0) {
                return;
            }
            tableSet = tableSet.startFlushingOnDisk();
        } finally {
            writeLock.unlock();
        }

        try {
            tasks.put(new FlushTask(snapshot.memTable, snapshot.generation));
        } catch (InterruptedException e) {
            logger.error("Cannot add memory table to flushing tasks", e);
            throw new IOException(e);
        }

    }

    List<Iterator<Cell>> getAllCellItersList(@NotNull final ByteBuffer from, @NotNull final List<Iterator<Cell>> iters, TableSet snapshot) {
        snapshot.ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iters.add(ssTable.iterator(from));
            } catch (IOException e) {
                logger.error("Something went wrong when the SSTable iterator was added to list iter!", e);
            }
        });

        return iters;
    }



    private Iterator<Cell> freshCellIterator(@NotNull final ByteBuffer from, @NotNull final List<Iterator<Cell>> itersList, TableSet snapshot) {
        final List<Iterator<Cell>> iters = getAllCellItersList(from, itersList, snapshot);

        final Iterator<Cell> mergedElements = Iterators.mergeSorted(
                iters,
                Cell.BY_KEY_AND_VALUE_CREATION_TIME_COMPARATOR
        );

        return Iters.collapseEquals(mergedElements, Cell::getKey);
    }

    private File serialize(final int generation, final Iterator<Cell> iterator) throws IOException {
        final File file = new File(storage, generation + SSTABLE_TEMPORARY_FILE_POSTFIX);
        file.createNewFile();
        SSTable.serialize(file, iterator);
        final String newFileName = generation + SSTABLE_FILE_POSTFIX;
        final File dst = new File(storage, newFileName);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        return dst;
    }

    @Override
    public TransactionalDAO beginTransaction() {
        return new TransactionalDAOImpl(this);
    }

}
