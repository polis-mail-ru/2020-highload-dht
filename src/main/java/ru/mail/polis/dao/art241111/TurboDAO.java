package ru.mail.polis.dao.art241111;

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

import static java.util.Objects.requireNonNull;

/**
 * Implementing a fast database.
 */
public class TurboDAO implements DAO {
    private static final String SUFFIX = "sst.dat";
    private static final String TEMP = "sst.tmp";
    private final long flushThreshold;
    private final File dir;
    private NavigableMap<Integer, Table> ssTables = new TreeMap<>();
    private MemTable memTable;
    private int generation;

    /**
     * Create the database.
     */
    public TurboDAO(@NotNull final File dir, final long flushThreshold) {
        this.memTable = new MemTable();
        this.flushThreshold = flushThreshold;
        this.dir = dir;

        generation = -1;
        final File[] list = dir.listFiles((dir1, name) -> name.endsWith(SUFFIX));
        assert list != null;

        Arrays.stream(list)
                .filter(file -> !file.isDirectory())
                .forEach(
                        f -> {
                            final String name = f.getName();
                            final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            try {
                                ssTables.put(gen, new SSTable(f));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            if (gen > generation) generation = gen;
                        }
                );
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final Iterator<Cell> alive = Iterators.filter(cellIterator(from),
                cell -> !requireNonNull(cell).getValue().isTombstone());
        return Iterators.transform(alive, cell -> Record.of(requireNonNull(cell).getKey(), cell.getValue().getData()));
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iters = new ArrayList<>(ssTables.size() + 1);
        iters.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(table -> iters.add(table.iterator(from)));

        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Comparator.naturalOrder());
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
        memTable.upsert(key, value);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        if (memTable.sizeInBytes() >= flushThreshold) {
            flush();
        }
        memTable.remove(key);
    }

    @Override
    public void close() throws IOException {
        if (memTable.getEntryCount() > 0) {
            flush();
        }
        ssTables.values().forEach(Table::close);
    }

    private void flush() throws IOException {
        final File tmp = new File(dir, generation + TEMP);
        SSTable.write(tmp, memTable.iterator(ByteBuffer.allocate(0)));

        final File dat = new File(dir, generation + SUFFIX);
        Files.move(tmp.toPath(), dat.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables.put(generation, new SSTable(dat));
        generation++;
        memTable = new MemTable();
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> iterator = cellIterator(ByteBuffer.allocate(0));
        final File tmp = new File(dir, generation + TEMP);
        SSTable.write(tmp, iterator);

        for (int i = 0; i < generation; i++) {
            Files.delete(new File(dir, i + SUFFIX).toPath());
        }
        generation = 0;

        final File file = new File(dir, generation + SUFFIX);
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables = new TreeMap<>();
        ssTables.put(generation, new SSTable(file));

        memTable = new MemTable();
        generation++;
    }
}

