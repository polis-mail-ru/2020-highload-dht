package ru.mail.polis.dao;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

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
import java.util.TreeMap;
import java.util.stream.Stream;

public class LsmDao implements DAO {
    private static final Logger log = LoggerFactory.getLogger(LsmDao.class);

    private static String SUFFIX = ".dat";
    private static String TEMP = ".tmp";

    @NotNull
    private final File storage;
    private final long flushThreshold;

    // Data
    private Table memTable;
    private final NavigableMap<Integer, Table> ssTables;

    // State
    private int generation;

    /**
     * Creates LsmDao.
     * @param storage database file storage
     * @param flushThreshold maximum size in bytes, data will be written to a file if size in bytes > flushThreshold
     * @throws IOException if an I/O error occurs
     */
    public LsmDao(@NotNull final File storage, final long flushThreshold) throws IOException {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                try {
                    // 3.dat
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    generation = Math.max(generation, gen);
                    ssTables.put(gen, new SSTable(f.toFile()));
                } catch (IOException e) {
                    // Log bad file
                    log.warn("Exception in 'new SSTable' ", e);
                } catch (NumberFormatException e) {
                    log.info("Incorrect file name: " + f.getFileName().toString());
                }
            });
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iters = new ArrayList<>(ssTables.size() + 1);
        iters.add(memTable.iterator(from));
        for (final Table t : ssTables.descendingMap().values()) {
            iters.add(t.iterator(from));
        }
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        // One cell per key
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        // No tombstones
        final Iterator<Cell> alive = Iterators.filter(fresh, e -> !e.getValue().isTombstone());
        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key.asReadOnlyBuffer(), value.asReadOnlyBuffer());
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key.asReadOnlyBuffer());
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
    }

    private void flush() throws IOException {
        // Dump memTable
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, memTable.iterator(ByteBuffer.allocate(0)));
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        // Switch
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() > 0) {
            flush();
        }

        for (final Table t : ssTables.values()) {
            t.close();
        }
    }

    private Iterator<Cell> aliveCells(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators = new ArrayList<>();
        for (final Table ssTable : this.ssTables.values()) {
            iterators.add(ssTable.iterator(from));
        }

        iterators.add(memTable.iterator(from));
        final Iterator<Cell> cellIterator = Iters.collapseEquals(
                Iterators.mergeSorted(iterators, Cell.COMPARATOR),
                Cell::getKey
        );

        return Iterators.filter(
                cellIterator, cell -> {
                    assert cell != null;
                    return !cell.getValue().isTombstone();
                }
        );
    }

    @Override
    public void compact() throws IOException {
        final File tmp = new File(storage, "compact" + TEMP);
        SSTable.serialize(tmp, aliveCells(ByteBuffer.allocate(0)));

        for (int i = 1; i < generation; i++) {
            Files.delete(Path.of(storage.toString() + "/" + i + SUFFIX));
        }

        generation = 1;
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(tmp.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables.clear();
        ssTables.put(generation, new SSTable(dst));
        memTable = new MemTable();
        generation++;
    }
}