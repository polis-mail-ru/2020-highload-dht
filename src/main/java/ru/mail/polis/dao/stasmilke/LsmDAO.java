package ru.mail.polis.dao.stasmilke;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.Record;

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
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * LSM implementation of {@link DAO}.
 */
public class LsmDAO implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".temp";
    private static final String COMPACT = "compact.temp";
    private static final int SSTABLES_LIMIT = 100;

    @NotNull
    private final File storage;
    private final long flushThreshold;

    private Table memTable;
    private final NavigableMap<Integer, Table> ssTables;
    private final Logger logger = LoggerFactory.getLogger(LsmDAO.class);
    private int generation;

    /**
     * Construct a {@link DAO} instance.
     *
     * @param storage local disk folder to persist the data to
     * @param flushThreshold max size of {@link MemTable}
     */
    public LsmDAO(
            @NotNull final File storage,
            final long flushThreshold) throws IOException {
        this.storage = storage;
        assert flushThreshold > 0L;
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(file -> {
                try {
                    final String name = file.getFileName().toString();
                    final int fileGeneration = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    generation = Math.max(fileGeneration, generation);
                    ssTables.put(fileGeneration, new SSTable(file.toFile()));
                } catch (NumberFormatException e) {
                    logger.warn(String.format("Incorrect name in file. %s", file.getFileName().toString()));
                } catch (IOException e) {
                    logger.warn("IOException in file. %s", e);
                    throw new UncheckedIOException(e);
                }
            });
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        // Removed tombstones
        final Iterator<Cell> alive = Iterators.filter(cellIterator(from, true), e -> !e.getValue().isTombstone());
        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @NotNull
    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from, final boolean addMemTable) throws IOException {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        if (addMemTable) {
            iterators.add(memTable.iterator(from));
        }
        ssTables.descendingMap().values().forEach(t -> {
            try {
                iterators.add(t.iterator(from));
                logger.debug("Iterator added");
            } catch (IOException e) {
                logger.warn("Error in iterators: %s", e);
                throw new RuntimeException("Error", e);
            }
        });
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell.COMPARATOR);
        // One cell per key
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
        if (generation > SSTABLES_LIMIT) {
            compact();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
        if (generation > SSTABLES_LIMIT) {
            compact();
        }
    }

    private void flush() throws IOException {
        final File file = fileForGeneration(generation, true);
        SSTable.serialize(
                file,
                memTable.iterator(ByteBuffer.allocate(0))
        );
        final File dst = fileForGeneration(generation, false);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        logger.info(String.format("Table has been flushed %d", generation));
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() > 0) {
            flush();
        }
        for (final Map.Entry<Integer, Table> entry : ssTables.entrySet()) {
            entry.getValue().close();
        }
    }

    @Override
    public void compact() throws IOException {
        final File tempFile = new File(storage, COMPACT);
        SSTable.serialize(
                tempFile,
                cellIterator(ByteBuffer.allocate(0), false)
        );
        for (int i = 1; i < generation; i++) {
            Files.delete(fileForGeneration(i, false).toPath());
        }
        final File dst = fileForGeneration(1, false);
        Files.move(tempFile.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.clear();
        ssTables.put(1, new SSTable(dst));
        generation = 2;
        logger.info("Table has been compacted");
    }

    private String nameForGeneration(final int gen, final boolean isTemp) {
        return storage.toString() + "/" + gen + (isTemp ? TEMP : SUFFIX);
    }

    private File fileForGeneration(final int gen, final boolean isTemp) {
        return new File(nameForGeneration(gen, isTemp));
    }
}
