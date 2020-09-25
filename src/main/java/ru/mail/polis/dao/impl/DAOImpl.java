package ru.mail.polis.dao.impl;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.tables.MemTable;
import ru.mail.polis.dao.impl.tables.SSTable;

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
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

public class DAOImpl implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    @NotNull
    private final File storage;
    private final long flushThreshold;
    private MemTable memTable;
    private final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>();
    private int generation;

    /**
     * Creates persistent DAO.
     *
     * @param storage folder to save and read data from
     * @throws IOException if cannot open or read SSTables
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold) throws IOException {
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.generation = 0;
        try (Stream<Path> stream = Files.walk(storage.toPath(), 1)) {
            stream.filter(path -> {
                final String name = path.getFileName().toString();
                return name.endsWith(SUFFIX)
                        && !path.toFile().isDirectory()
                        && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$"); })
                    .forEach(path -> {
                        try {
                            final String name = path.getFileName().toString();
                            final int currentGeneration = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            this.generation = Math.max(this.generation, currentGeneration);
                            ssTables.put(currentGeneration, new SSTable(path.toFile()));
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(cellIterator(from),
                cell -> Record.of(Objects.requireNonNull(cell).getKey(), cell.getValue().getData()));
    }

    /**
     * Create an iterator over alive {@link Cell}.
     *
     * @param from data on which iterator is created
     * @return an iterator over alive cells
     */
    @NotNull
    public Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> fileIterators = new ArrayList<>(ssTables.size() + 1);
        fileIterators.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        final Iterator<Cell> cells = Iterators.mergeSorted(fileIterators, Comparator.naturalOrder());
        final Iterator<Cell> fresh = Iters.collapseEquals(cells, Cell::getKey);
        return Iterators.filter(
                fresh, cell -> !Objects.requireNonNull(cell).getValue().isTombstone());
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (memTable.getEntryCount() > 0) {
            flush();
        }
        ssTables.values().forEach(SSTable::close);
    }

    private void flush() throws IOException {
        final File temp = new File(storage, generation + TEMP);
        SSTable.write(memTable.iterator(ByteBuffer.allocate(0)), temp);
        final File file = new File(storage, generation + SUFFIX);
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(file));
        generation++;
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> cells = cellIterator(ByteBuffer.allocate(0));
        final File temp = new File(storage, generation + TEMP);
        SSTable.write(cells, temp);

        for (final Map.Entry<Integer, SSTable> ssTableEntry : ssTables.entrySet()) {
            ssTableEntry.getValue().deleteSSTableFile();
        }
        ssTables.clear();
        generation = 0;
        final File file = new File(storage, generation + SUFFIX);
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.put(generation, new SSTable(file));
        memTable = new MemTable();
        generation++;
    }
}
