package ru.mail.polis.dao.kirkungurov;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class DAOImpl implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    private static final int MAX_FILES_COUNT = 50;

    @NotNull
    private final File storage;
    private final long tableByteSize;

    @NotNull
    private MemTable memTable;
    @NotNull
    private final NavigableMap<Integer, Table> ssTables;

    private int generation;

    /**
     * DAO implementation.
     *
     * @param storage - direction where SSTable stored
     * @param tableByteSize - amount of bytes which needed to memtable
     */
    public DAOImpl(final File storage, final long tableByteSize) {
        this.storage = storage;
        this.tableByteSize = tableByteSize;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        this.generation = -1;

        final File[] list = storage.listFiles((dir1, name) -> name.endsWith(SUFFIX));
        assert list != null;
        Arrays.stream(list)
                .filter(curFile -> !curFile.isDirectory())
                .forEach(file -> {
                    final String name = file.getName();
                    final String nameWithoutSuf = name.substring(0, name.indexOf(SUFFIX));
                    if (nameWithoutSuf.matches("[0-9]+")) {
                        final int version = Integer.parseInt(nameWithoutSuf);
                        generation = Math.max(generation, version);
                        try {
                            ssTables.put(version, new SSTable(file));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
        generation++;
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
                throw new RuntimeException(e);
            }
        });

        final Iterator<Cell> mergedElements = Iterators.mergeSorted(iterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        final Iterator<Cell> freshElements = Iters.collapseEquals(mergedElements, Cell::getKey);
        final Iterator<Cell> archiveElements = Iterators.filter(freshElements,
                element -> !element.getValue().isTombstone());
        return Iterators.transform(archiveElements,
                element -> Record.of(element.getKey(), element.getValue().getData()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.size() >= tableByteSize) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.size() >= tableByteSize) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() >= 0) {
            flush();
        }
        for (final Table table : ssTables.values()) {
            table.close();
        }
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> iterator = cellIterator(ByteBuffer.allocate(0));
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, iterator);
        for (int i = 0; i < ssTables.size(); i++) {
            Files.delete(new File(storage, i + SUFFIX).toPath());
        }
        generation = 0;
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        generation++;
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iterators = new ArrayList<>(generation + 1);
        for (final Table table : ssTables.values()) {
            iterators.add(table.iterator(from));
        }
        iterators.add(memTable.iterator(from));
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell::compareTo);
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    private void flush() throws IOException {
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, memTable.iterator(ByteBuffer.allocate(0)));
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        if (ssTables.size() > MAX_FILES_COUNT) {
            compact();
        }
        generation++;
    }
}
