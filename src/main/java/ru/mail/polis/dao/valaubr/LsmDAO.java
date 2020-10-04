package ru.mail.polis.dao.valaubr;

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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * DAO implementation.
 * Persistence storage.
 *
 * @author Ilya Chuprynin
 */
public class LsmDAO implements DAO {

    private static final String FILE_POSTFIX = ".dat";
    private static final String TEMP_FILE_POSTFIX = ".tmp";
    private static final String LSM_TEMP_FILE = "temp.tmp";

    @NotNull
    private final File storage;
    private final long flushThreshold;

    private MemTable memtable;
    private final NavigableMap<Integer, Table> ssTables;

    private int generation;

    private final Pattern pattern = Pattern.compile("^\\d+$");

    /**
     * DAO constructor for storage file with size limit.
     *
     * @param storage        the path to the file where the files will be stored
     * @param flushThreshold max table size
     * @throws IOException file work exception
     */
    public LsmDAO(
            @NotNull final File storage,
            final long flushThreshold) throws IOException {
        assert flushThreshold > 0L;
        this.storage = storage;
        this.flushThreshold = flushThreshold;
        this.memtable = new MemTable();
        this.ssTables = new TreeMap<>();
        try (Stream<Path> files = Files.list(storage.toPath())) {
            files.filter(file -> !file.toFile().isDirectory() && file.toString().endsWith(FILE_POSTFIX))
                    .forEach(file -> {
                        final String fileName = file.getFileName().toString();
                        try {
                            if (pattern.matcher(fileName.substring(0, fileName.indexOf(FILE_POSTFIX))).find()) {
                                final int gen = Integer.parseInt(fileName.substring(0, fileName.indexOf(FILE_POSTFIX)));
                                generation = Math.max(gen, generation);
                                ssTables.put(gen, new SSTable(file.toFile()));
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            generation++;
        }
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
        iters.add(memtable.iterator(from));
        ssTables.descendingMap().values().forEach(table -> {
            try {
                iters.add(table.iterator(from));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        final Iterator<Cell> merged = Iterators.mergeSorted(iters, Cell.COMPARATOR);
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        memtable.upsert(key, value);
        if (memtable.getSizeInByte() > flushThreshold) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memtable.remove(key);
        if (memtable.getSizeInByte() > flushThreshold) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (memtable.size() > 0) {
            flush();
        }
        ssTables.values().forEach(Table::close);
    }

    private void flush() throws IOException {
        final File file = new File(storage, generation + TEMP_FILE_POSTFIX);
        file.createNewFile();
        SSTable.serialize(file, memtable.iterator(ByteBuffer.allocate(0)));
        final File dst = new File(storage, generation + FILE_POSTFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ++generation;
        ssTables.put(generation, new SSTable(dst));
        memtable.close();
    }

    @Override
    public void compact() throws IOException {
        final File tempFile = new File(storage, LSM_TEMP_FILE);
        tempFile.createNewFile();
        SSTable.serialize(
                tempFile,
                cellIterator(ByteBuffer.allocate(0))
        );
        for (int i = 1; i < generation; i++) {
            Files.delete(new File(storage, i + FILE_POSTFIX).toPath());
        }
        generation = 1;
        final File dst = new File(storage, 1 + FILE_POSTFIX);
        dst.createNewFile();
        Files.move(tempFile.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.clear();
        ssTables.put(generation++, new SSTable(dst));
        memtable = new MemTable();
    }
}

