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

    private TableSet tableSet;

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
        final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>();
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
        this.tableSet = TableSet.fromFiles(ssTables, generation + 1);
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final ByteBuffer key = from.rewind().duplicate();
        final Iterator<Cell> alive = getAliveCells(key);

        return Iterators.transform(alive, el -> Record.of(el.getKey(), el.getValue().getContent()));
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> alive = getAliveCells(ByteBuffer.allocate(0));

        final TableSet snapshot = this.tableSet;

        this.tableSet = this.tableSet.startCompacting();

        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(file, alive);

        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        this.tableSet= this.tableSet.replaceCompactedFiles(snapshot.files, new SSTable(dst), snapshot.generation);

        for (int i = generation - 1; i >= 0; i--) {
            final File delFile = new File(storage, i + SUFFIX);
            //final SSTable ssTable = tableSet.files.remove(i);
//            if (ssTable == null) {
//                break;
//            }
//            ssTable.close();
            try {
                Files.delete(delFile.toPath());
            } catch (NoSuchFileException e) {
                break;
            }
        }

        generation = 0;
        //atomicMoveTempToSuffix(file, snapshot);
    }

    private Iterator<Cell> getAliveCells(@NotNull final ByteBuffer key) throws IOException {

        final TableSet snapshot = this.tableSet;


        final List<Iterator<Cell>> iterators = new ArrayList<>(snapshot.files.size() + 1);
        iterators.add(snapshot.mem.iterator(key));
        snapshot.files.descendingMap().values().forEach(ssTable -> {
            iterators.add(ssTable.iterator(key));
        });
        for (final Table flushing : snapshot.flushing) {
            iterators.add(flushing.iterator(key));
        }
        final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell.COMPARATOR);
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        return Iterators.filter(fresh, el -> el.getValue().getContent() != null);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        tableSet.mem.upsert(key.rewind().duplicate(), value.rewind().duplicate());
        if (tableSet.mem.getSizeInBytes() > flushSize) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        tableSet.mem.remove(key.rewind().duplicate());
        if (tableSet.mem.getSizeInBytes() > flushSize) {
            flush();
        }
    }

    private void flush() throws IOException {

        final TableSet snapshot = this.tableSet;


        if (snapshot.mem.size() == 0) {
            return;
        }
        this.tableSet = snapshot.markedAsFlushing();
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(
                file,
                snapshot.mem.iterator(ByteBuffer.allocate(0)));

        atomicMoveTempToSuffix(file, snapshot);
    }

    private void atomicMoveTempToSuffix(final File file, final TableSet snapshot) throws IOException {
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        this.tableSet = this.tableSet.flushed(snapshot.mem, new SSTable(dst), generation);
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (tableSet.mem.size() > 0) {
            flush();
        }
        tableSet.files.values().forEach(SSTable::close);
    }
}
