package ru.mail.polis.dao.kate.moreva;

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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
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
    private static final int MAX_NUMBER_OF_FILES = 100;

    private static final Logger log = LoggerFactory.getLogger(SSTable.class);

    @NotNull
    private final File storage;

    @NotNull
    private MemTable memTable;

    @NotNull
    private final NavigableMap<Integer, Table> ssTables;

    private final long flushThreshold;
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
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.ssTables = new ConcurrentSkipListMap<>();
        this.generation = -1;

        try (Stream<Path> stream = Files.list(storage.toPath())) {
            stream
                    .filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SUFFIX)
                                && !name.substring(0, name.indexOf(SUFFIX)).matches(LETTERS)
                                && !path.toFile().isDirectory();
                    })
                    .forEach(this::storeData);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        generation++;
    }

    private void storeData(@NotNull final Path path) {
        try {
            final String fileName = path.getFileName().toString();
            final int generationCounter = Integer.parseInt(
                    fileName.substring(0, fileName.indexOf(SUFFIX)));
            generation = Math.max(generation, generationCounter);
            ssTables.put(generationCounter, new SSTable(path));
        } catch (NumberFormatException e) {
            log.error("Wrong name", e);
        }
    }

    private void flush() throws IOException {
        final File file = new File(storage, generation + TMP);
        SSTable.serialize(file, memTable.iterator(ByteBuffer.allocate(0)));

        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables.put(generation, new SSTable(dst.toPath()));
        generation++;
        memTable.close();
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        iterators.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(table -> {
            try {
                iterators.add(table.iterator(from));
            } catch (IOException e) {
                log.error("Adding iterator error", e);
            }
        });
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Comparator.naturalOrder());
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        iterators.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iterators.add(ssTable.iterator(from));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

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
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value.asReadOnlyBuffer());
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
        if (ssTables.size() >= MAX_NUMBER_OF_FILES) {
            compact();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
        if (ssTables.size() >= MAX_NUMBER_OF_FILES) {
            compact();
        }
    }

    @Override
    public void close() throws IOException {
        if (memTable.sizeInBytes() > 0) {
            flush();
        }
        ssTables.values().forEach(table -> {
            try {
                table.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> iterator = cellIterator(ByteBuffer.allocate(0));
        final File tmpFile = new File(storage, generation + TMP);
        SSTable.serialize(tmpFile, iterator);
        for (int i = 0; i < generation; i++) {
            Files.delete(new File(storage, i + SUFFIX).toPath());
        }
        generation = 0;
        final File datFile = new File(storage, generation + SUFFIX);
        Files.move(tmpFile.toPath(), datFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(datFile.toPath()));
        generation++;
    }
}

