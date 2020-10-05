package ru.mail.polis.dao.nik27090;

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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class DAOImpl implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    private MemTable memTable;
    private final NavigableMap<Integer, SSTable> ssTables;

    @NotNull
    private final File storage;
    private final long flushSize;

    private int generation;

    /**
     * Creates key-value database.
     *
     * @param storage data storage
     * @param flushSize MemTable size at which MemTable converted to SSTable
     */
    public DAOImpl(
            @NotNull final File storage,
            final long flushSize) throws IOException {
        this.storage = storage;
        this.flushSize = flushSize;
        assert flushSize > 0L;
        this.ssTables = new TreeMap<>();
        this.memTable = new MemTable();
        this.generation = -1;

        try (Stream<Path> paths = Files.list(storage.toPath())) {
            paths
                    .filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SUFFIX)
                                && !path.toFile().isDirectory()
                                && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$");
                    })
                    .forEach(element -> {
                        try {
                            final String name = element.getFileName().toString();
                            final int foundedGen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            this.generation = Math.max(this.generation, foundedGen);
                            ssTables.put(foundedGen, new SSTable(element.toFile()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final ByteBuffer key = from.rewind().duplicate();
        final Iterator<Cell> alive = getAliveCells(key);

        return Iterators.transform(alive, el -> Record.of(el.getKey(), el.getValue().getContent()));
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> alive = getAliveCells(ByteBuffer.allocate(0));

        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, alive);

        for (int i = generation - 1; i >= 0; i--) {
            final File delFile = new File(storage, i + SUFFIX);
            final SSTable ssTable = ssTables.remove(i);
            if (ssTable == null) {
                break;
            }
            ssTable.close();
            try {
                Files.delete(delFile.toPath());
            } catch (NoSuchFileException e) {
                break;
            }
        }

        generation = 0;
        atomicMoveTempToSuffix(file);
    }

    private Iterator<Cell> getAliveCells(@NotNull final ByteBuffer key) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        iterators.add(memTable.iterator(key));
        ssTables.descendingMap().values().forEach(ssTable -> {
            iterators.add(ssTable.iterator(key));
        });
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell.COMPARATOR);
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        return Iterators.filter(fresh, el -> el.getValue().getContent() != null);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key.rewind().duplicate(), value.rewind().duplicate());
        if (memTable.getSizeInBytes() > flushSize) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key.rewind().duplicate());
        if (memTable.getSizeInBytes() > flushSize) {
            flush();
        }
    }

    private void flush() throws IOException {
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(
                file,
                memTable.iterator(ByteBuffer.allocate(0)));

        atomicMoveTempToSuffix(file);
    }

    private void atomicMoveTempToSuffix(final File file) throws IOException {
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() > 0) {
            flush();
        }
        ssTables.values().forEach(SSTable::close);
    }
}
