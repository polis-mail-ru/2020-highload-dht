package ru.mail.polis.dao.gogun;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DAOImpl implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    private MemTable memTable;
    private final NavigableMap<Integer, SSTable> ssTables;

    @NotNull
    private final File storage;
    private final long flushThreshold;
    private static final Logger logger = Logger.getLogger(DAOImpl.class.getName());
    private int generation;

    /**
     * implementation of lsm.
     *
     * @param storage directory with sstables
     * @param flushThreshold threshold to write data
     * @throws IOException io error
     */
    public DAOImpl(@NotNull final File storage, final long flushThreshold) throws IOException {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(path -> path.toString().endsWith(SUFFIX)).forEach(f -> {
                try {
                    final String name = f.getFileName().toString();
                    final int gen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                    this.generation = Math.max(this.generation, gen);
                    ssTables.put(gen, new SSTable(f.toFile()));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "ctor bug", e);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "bad name", e);
                }
            });
        }
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final Iterator<Row> fresh = rowIterator(from);
        final Iterator<Row> alive = Iterators.filter(fresh, e -> !e.getValue(). isTombstone());

        return Iterators.transform(alive, e -> Record.of(e.getKey(), e.getValue().getData()));
    }

    @NotNull
    private Iterator<Row> rowIterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Row>> iters = new ArrayList<>(ssTables.size() + 1);
        iters.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(t -> {
            try {
                iters.add(t.iterator(from));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "iter fail", e);
            }
        });
        final Iterator<Row> merged = Iterators.mergeSorted(iters, Row.COMPARATOR);

        return Iters.collapseEquals(merged, Row::getKey);
    }

    @Override
    public void compact() throws IOException {
        //get fresh rows
        final Iterator<Row> freshRows = rowIterator(ByteBuffer.allocate(0));
        final File file = new File(storage, generation + TEMP);
        //save them
        SSTable.serialize(file, freshRows);
        //delete old
        for (final SSTable table : ssTables.values()) {
            table.deleteFile();
        }
        ssTables.clear();

        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.put(generation, new SSTable(dst));

        generation = ssTables.size() + 1;
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.getSizeInBytes() > flushThreshold) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.getSizeInBytes() > flushThreshold) {
            flush();

        }
    }

    private void flush() throws IOException {
        final File file = new File(storage, generation + TEMP);
        SSTable.serialize(
                file,
                memTable.iterator(ByteBuffer.allocate(0)));
        final File dst = new File(storage, generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        memTable = new MemTable();
        ssTables.put(generation, new SSTable(dst));
        generation++;
    }

    @Override
    public void close() throws IOException {
        if (memTable.getSize() > 0) {
            flush();
        }
        for (final Entry<Integer, SSTable> elem : ssTables.entrySet()) {
            elem.getValue().close();
        }
    }
}
