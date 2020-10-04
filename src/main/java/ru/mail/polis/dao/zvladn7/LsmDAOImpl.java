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
import java.util.concurrent.ConcurrentSkipListMap;
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
    private TableSet tableSet;

    /**
     * LSM DAO implementation.
     * @param storage - the directory where SSTables stored.
     * @param amountOfBytesToFlush - amount of bytes that need to flush current memory table.
     */
    public LsmDAOImpl(@NotNull final File storage, final int amountOfBytesToFlush) throws IOException {
        this.storage = storage;
        this.amountOfBytesToFlush = amountOfBytesToFlush;
        NavigableMap<Integer, Table> ssTables = new ConcurrentSkipListMap<>();
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
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iters = new ArrayList<>(tableSet.ssTables.size() + 2);
        iters.add(tableSet.memTable.iterator(from));
        final Iterator<Cell> freshElements = freshCellIterator(from, iters);
        final Iterator<Cell> aliveElements = Iterators.filter(freshElements, el -> !el.getValue().isTombstone());

        return Iterators.transform(aliveElements, el -> Record.of(el.getKey(), el.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        tableSet.memTable.upsert(key, value);
        if (tableSet.memTable.getAmountOfBytes() > amountOfBytesToFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        tableSet.memTable.remove(key);
        if (tableSet.memTable.getAmountOfBytes() > amountOfBytesToFlush) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (tableSet.memTable.size() > 0) {
            flush();
        }
        tableSet.ssTables.values().forEach(Table::close);
    }

    @Override
    public void compact() throws IOException {
        final List<Iterator<Cell>> iters = new ArrayList<>(tableSet.ssTables.size());
        final Iterator<Cell> freshElements = freshCellIterator(EMPTY_BUFFER, iters);
        final TableSet snapsnot = tableSet;
        tableSet = tableSet.startCompact();
        final File dst = serialize(snapsnot, freshElements);

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
        tableSet = tableSet.finishCompact(snapsnot.ssTables, dst);
    }

    private void flush() throws IOException {
        final TableSet snapshot = tableSet;
        tableSet = tableSet.startFlushingOnDisk();
        final File dst = serialize(snapshot, snapshot.memTable.iterator(EMPTY_BUFFER));
        tableSet = tableSet.finishFlushingOnDisk(snapshot.memTable, dst);

    }

    List<Iterator<Cell>> getAllCellItersList(@NotNull final ByteBuffer from, @NotNull final List<Iterator<Cell>> iters) {
        tableSet.ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iters.add(ssTable.iterator(from));
            } catch (IOException e) {
                logger.error("Something went wrong when the SSTable iterator was added to list iter!", e);
            }
        });

        return iters;
    }



    private Iterator<Cell> freshCellIterator(@NotNull final ByteBuffer from, @NotNull final List<Iterator<Cell>> itersList) {
        final List<Iterator<Cell>> iters = getAllCellItersList(from, itersList);

        final Iterator<Cell> mergedElements = Iterators.mergeSorted(
                iters,
                Cell.BY_KEY_AND_VALUE_CREATION_TIME_COMPARATOR
        );

        return Iters.collapseEquals(mergedElements, Cell::getKey);
    }

    private File serialize(final TableSet snapshot, final Iterator<Cell> iterator) throws IOException {
        final File file = new File(storage, snapshot.generation + SSTABLE_TEMPORARY_FILE_POSTFIX);
        file.createNewFile();
        SSTable.serialize(file, iterator);
        final String newFileName = snapshot.generation + SSTABLE_FILE_POSTFIX;
        final File dst = new File(storage, newFileName);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        return dst;
    }

    @Override
    public TransactionalDAO beginTransaction() {
        return new TransactionalDAOImpl(this);
    }

}
