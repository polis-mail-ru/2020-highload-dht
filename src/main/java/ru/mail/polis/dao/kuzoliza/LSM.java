package ru.mail.polis.dao.kuzoliza;

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

public class LSM implements DAO {

    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    private MemTable memTable;
    private final NavigableMap<Integer, SStable> ssTables;

    // Data
    private final File storage;
    private final long flushThreshold;
    // State
    private int generation;

    /**
     * Key Value DB.
     *
     * @param storage data storage
     * @param flushThreshold memTable size (when size == flushThreshold memTable is converted into ssTable)
     * @throws IOException may appear exception
     */
    public LSM(@NotNull final File storage, final long flushThreshold) throws IOException {
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        assert flushThreshold > 0L;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        this.generation = -1;

        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> {
                final String name = path.getFileName().toString();
                return name.endsWith(SUFFIX) && !path.toFile().isDirectory()
                        && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$");
            }).forEach(f -> {
                try {
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    this.generation = Math.max(this.generation, gen);
                    ssTables.put(gen, new SStable(f.toFile()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final Iterator<Cell> alive = noTombstones(from);
        return Iterators.transform(alive, e -> {
            assert e != null;
            return Record.of(e.getKey(), e.getValue().getData());
        });

    }

    private Iterator<Cell> noTombstones(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> iters = new ArrayList<>(ssTables.size() + 1);
        iters.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(t -> {
            iters.add(t.iterator(from));
        });
        // Sorted duplicates and tombstones
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        // One cell per key
        final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
        // No tombstones
        return Iterators.filter(fresh, e -> {
            assert e != null;
            return e.getValue().getData() != null;
        });
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> alive = noTombstones(ByteBuffer.allocate(0));

        final File file = new File(storage, generation + TEMP);
        SStable.serialize(file, alive);

        for (int i = generation - 1; i >= 0; i--) {
            final File removeFile = new File(storage, i + SUFFIX);
            final SStable ssTable = ssTables.remove(i);
            if (ssTable == null) {
                break;
            }
            ssTable.close();
            try {
                Files.delete(removeFile.toPath());
            } catch (NoSuchFileException e) {
                break;
            }
        }

        generation = 0;
        moveAndSwitch(file);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.sizeInBytes() > flushThreshold) {
            flush();
        }
    }

    private void flush() throws IOException {
        // Dump memTable
        final File file = new File(storage, generation + TEMP);
        SStable.serialize(file, memTable.iterator(ByteBuffer.allocate(0)));
        moveAndSwitch(file);
    }

    private void moveAndSwitch(final File file) throws IOException {
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        //Switch
        memTable = new MemTable();
        ssTables.put(generation, new SStable(dst));
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() > 0) {
            flush();
        }
        ssTables.values().forEach(SStable::close);
    }
}
