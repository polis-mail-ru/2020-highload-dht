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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LsmDAOImpl implements LsmDAO {

    private static final Logger logger = LoggerFactory.getLogger(LsmDAOImpl.class);

    static final String SSTABLE_FILE_POSTFIX = ".dat";
    private static final String SSTABLE_COMPACTED_FILE_POSTFIS = ".compacted";
    private static final String SSTABLE_TEMPORARY_FILE_POSTFIX = ".tmp";

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private static final Integer FILES_TO_COMPACT = 5;

    @NonNull
    private final File storage;
    private final int amountOfBytesToFlush;
    final Map<ByteBuffer, Long> lockTable = new HashMap<>();

    TableSet tableSet;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    @NotNull
    private final ExecutorService service;
    private final AtomicInteger allowedCompactions = new AtomicInteger(2);
    private final AtomicInteger compactedFilePointer = new AtomicInteger();

    /**
     * LSM DAO implementation.
     *
     * @param storage              - the directory where SSTables stored.
     * @param amountOfBytesToFlush - amount of bytes that need to flush current memory table.
     */
    public LsmDAOImpl(
            @NotNull final File storage,
            final int amountOfBytesToFlush,
            final int flushQueueSize) throws IOException {
        this.storage = storage;
        this.amountOfBytesToFlush = amountOfBytesToFlush;
        final NavigableMap<Integer, Table> ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(SSTABLE_FILE_POSTFIX))
                    .forEach(file -> {
                        final String fileName = file.getFileName().toString();
                        try {
                            final String stringGen = fileName.substring(0, fileName.indexOf(SSTABLE_FILE_POSTFIX));
                            final int gen = Integer.parseInt(stringGen);
                            ssTables.put(gen, new SSTable(file.toFile()));
                        } catch (IOException e) {
                            logger.error("Something went wrong while the SSTable was created!", e);
                        } catch (NumberFormatException e) {
                            logger.info("Unexpected name of SSTable file: " + fileName, e);
                        }
                    });
        }

        final Map.Entry<Integer, Table> genEntry = ssTables.entrySet()
                .stream()
                .max(Comparator.comparing(Map.Entry::getKey))
                .orElse(null);
        int generation = 0;
        if (genEntry != null) {
            generation = genEntry.getKey();
        }

        this.tableSet = TableSet.provideTableSet(ssTables, generation + 1);
        this.service = Executors.newFixedThreadPool(flushQueueSize);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        readLock.lock();
        try {
            snapshot = this.tableSet;
            this.tableSet = this.tableSet.startIterating();
        } finally {
            readLock.unlock();
        }
        final List<Iterator<Cell>> iters = uniteIters(snapshot, from);
        snapshot.ssTables.putAll(snapshot.ssTablesToCompact);
        final Iterator<Cell> freshElements = freshCellIterator(from, iters, snapshot.ssTables);
        final Iterator<Cell> aliveElements = Iterators.filter(freshElements, el -> !el.getValue().isTombstone());

        return Iterators.transform(aliveElements, el -> Record.of(el.getKey(), el.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        execute(() -> tableSet.memTable.upsert(key, value));
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        execute(() -> tableSet.memTable.remove(key));
    }

    @Override
    public void compact() {
        compactWithCondition(false);
    }

    private void compactWithCondition(final boolean inFlush) {
        final boolean isEmptyListOfFiles;
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
        final NavigableMap<Integer, Table> ssTablesToCompact;
        final int genToSave;
        writeLock.lock();
        try {
            snapshot = tableSet;
            if (inFlush) {
                ssTablesToCompact = startCompact(compactedFilePointer.get(), 5);
            } else {
                ssTablesToCompact = startCompact();
            }
            genToSave = ssTablesToCompact.lastEntry().getKey();
        } finally {
            writeLock.unlock();
        }

        service.execute(() -> {
            logger.debug("Compacting byte(s) to {}", snapshot.generation);
            try {
                final List<Iterator<Cell>> iters = new ArrayList<>(ssTablesToCompact.size());
                final Iterator<Cell> freshElements = freshCellIterator(EMPTY_BUFFER, iters, ssTablesToCompact);
                final File dst = serialize(genToSave, freshElements, SSTABLE_COMPACTED_FILE_POSTFIS);

                try (Stream<Path> files = Files.list(storage.toPath())) {
                    files.filter(f -> {
                        final String name = f.getFileName().toFile().toString();
                        final int gen = Integer.parseInt(name.substring(0, name.indexOf('.')));
                        final boolean correctPostfix = name.endsWith(SSTABLE_FILE_POSTFIX);
                        final boolean isCurrentCompact = ssTablesToCompact.containsKey(gen);
                        return gen < snapshot.generation && correctPostfix && isCurrentCompact;
                    }).forEach(f -> {
                        try {
                            Files.delete(f);
                        } catch (IOException e) {
                            logger.warn("Unable to delete file: " + f.getFileName().toFile().toString(), e);
                        }
                    });
                }

                logger.debug("Compacted byte(s) to {}", snapshot.generation);
                writeLock.lock();
                try {
                    tableSet = tableSet.finishCompact(ssTablesToCompact, dst, genToSave, storage);
                    if (allowedCompactions.get() < 2) {
                        allowedCompactions.incrementAndGet();
                    }
                    compactedFilePointer.incrementAndGet();
                } finally {
                    writeLock.unlock();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private NavigableMap<Integer, Table> startCompact(int startPosition, int amount) {
        final NavigableMap<Integer, Table> newSSTablesToCompact = new TreeMap<>(tableSet.ssTablesToCompact);
        final NavigableMap<Integer, Table> toCompact = new TreeMap<>();
        final NavigableMap<Integer, Table> newSSTable = new TreeMap<>();
        for (Map.Entry<Integer, Table> next : tableSet.ssTables.entrySet()) {
            if (startPosition == 0 && amount != 0) {
                amount--;
                toCompact.put(next.getKey(), next.getValue());
            } else {
                if (startPosition != 0) {
                    startPosition--;
                }
                newSSTable.put(next.getKey(), next.getValue());
            }
        }
        newSSTablesToCompact.putAll(toCompact);
        tableSet = tableSet.startCompact(newSSTablesToCompact, newSSTable);
        return toCompact;
    }

    private NavigableMap<Integer, Table> startCompact() {
        final NavigableMap<Integer, Table> newSSTablesToCompact = new TreeMap<>(tableSet.ssTablesToCompact);
        final NavigableMap<Integer, Table> toCompact = tableSet.ssTables;
        newSSTablesToCompact.putAll(toCompact);
        tableSet = tableSet.startCompact(newSSTablesToCompact, new TreeMap<>());
        return toCompact;
    }

    private void flush() {
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
        service.execute(() -> {
            try {
                logger.debug("Flushing {} bytes(s) to {}", snapshot.memTable.getAmountOfBytes(), snapshot.generation);
                final File dst = serialize(snapshot.generation,
                        snapshot.memTable.iterator(EMPTY_BUFFER),
                        SSTABLE_FILE_POSTFIX);
                writeLock.lock();
                try {
                    tableSet = tableSet.finishFlushingOnDisk(snapshot.memTable, dst, snapshot.generation);
                } finally {
                    writeLock.unlock();
                }
                logger.debug("Flushed {} bytes(s) to {}", snapshot.memTable.getAmountOfBytes(), snapshot.generation);
            } catch (IOException e) {
                logger.error("Cannot flush memory table on disk", e);
                Runtime.getRuntime().halt(-1);
            }
        });
        final int ssTablesSize;
        writeLock.lock();
        try {
            ssTablesSize = tableSet.ssTables.size() - compactedFilePointer.get();
        } finally {
            writeLock.unlock();
        }
        if (ssTablesSize >= FILES_TO_COMPACT && allowedCompactions.get() > 0) {
            allowedCompactions.decrementAndGet();
            compactWithCondition(true);
        }
    }

    @Override
    public void close() {
        final boolean isReadyToFlush;
        readLock.lock();
        try {
            isReadyToFlush = tableSet.memTable.size() > 0;
        } finally {
            readLock.unlock();
        }

        if (isReadyToFlush) {
            flush();
        }
        service.shutdown();
        while (true) {
            if (service.isTerminated()) {
                break;
            }
        }
        readLock.lock();
        try {
            tableSet.ssTables.values().forEach(Table::close);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public TransactionalDAO beginTransaction() {
        return new TransactionalDAOImpl(this);
    }

    private Iterator<Cell> freshCellIterator(@NotNull final ByteBuffer from,
                                             @NotNull final List<Iterator<Cell>> itersList,
                                             final NavigableMap<Integer, Table> ssTables) {
        final List<Iterator<Cell>> iters = getAllCellItersList(from, itersList, ssTables);

        final Iterator<Cell> mergedElements = Iterators.mergeSorted(
                iters,
                Cell.BY_KEY_AND_VALUE_CREATION_TIME_COMPARATOR
        );

        return Iters.collapseEquals(mergedElements, Cell::getKey);
    }

    private File serialize(final int generation, final Iterator<Cell> iterator, final String postfix) throws IOException {
        final File file = new File(storage, generation + SSTABLE_TEMPORARY_FILE_POSTFIX);
        SSTable.serialize(file, iterator);
        final String newFileName = generation + postfix;
        final File dst = new File(storage, newFileName);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        return dst;
    }

    static List<Iterator<Cell>> uniteIters(final TableSet snapshot, final ByteBuffer from) {
        final List<Iterator<Cell>> iters = new ArrayList<>();
        iters.add(snapshot.memTable.iterator(from));
        snapshot.memToFlush.forEach(mem -> iters.add(mem.iterator(from)));

        return iters;
    }

    List<Iterator<Cell>> getAllCellItersList(@NotNull final ByteBuffer from,
                                             @NotNull final List<Iterator<Cell>> iters,
                                             final NavigableMap<Integer, Table> ssTables) {
        ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iters.add(ssTable.iterator(from));
            } catch (IOException e) {
                logger.error("Something went wrong when the SSTable iterator was added to list iter!", e);
            }
        });

        return iters;
    }

    private void execute(final Runnable task) {
        final boolean isReadyToFlush;
        readLock.lock();
        try {
            task.run();
            isReadyToFlush = tableSet.memTable.getAmountOfBytes() > amountOfBytesToFlush;
        } finally {
            readLock.unlock();
        }
        if (isReadyToFlush) {
            flush();
        }
    }
}
