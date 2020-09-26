package ru.mail.polis.dao.bmendli;

import com.google.common.collect.Iterators;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

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
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

/**
 * Persistent storage.
 *
 * @author bmendli
 */
public class DAOImpl implements DAO {

    private static final String SSTABLE_FILE_END = ".dat";
    private static final String SSTABLE_TMP_FILE_END = ".tmp";
    private static final String FILE_NAME_REGEX = "[0-9]+";

    @NonNull
    private final File storage;
    @NotNull
    private final MemTable memTable;
    @NotNull
    private final NavigableMap<Integer, Table> ssTables;
    private final long tableByteSize;

    private int generation;

    /**
     * Creates DAO from file storage, data from file will be store in immutable SSTable
     * and new data - MemTable.
     *
     * @param storage       - file in which store data
     * @param tableByteSize - max table byte size
     */
    public DAOImpl(final File storage, final long tableByteSize) {
        this.storage = storage;
        this.tableByteSize = tableByteSize;
        this.memTable = new MemTable();
        this.ssTables = new TreeMap<>();
        this.generation = -1;

        try (Stream<Path> stream = Files.walk(storage.toPath(), 1)) {
            stream
                    .filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SSTABLE_FILE_END)
                                && !path.toFile().isDirectory()
                                && name.substring(0, name.indexOf(SSTABLE_FILE_END)).matches(FILE_NAME_REGEX);
                    })
                    .forEach(this::storeDataFromFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        generation++;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final Iterator<Cell> filteredIterator = Iterators.filter(cellIterator(from),
                cell -> !cell.getValue().isTombstone() && !cell.getValue().isExpired());
        return Iterators.transform(filteredIterator, cell -> Record.of(cell.getKey(), cell.getValue().getData()));
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value,
            final long expireTime) throws IOException {
        memTable.upsert(key.asReadOnlyBuffer(), value.asReadOnlyBuffer(), expireTime);
        if (memTable.size() >= tableByteSize) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key.asReadOnlyBuffer());
        if (memTable.size() >= tableByteSize) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (memTable.size() > 0) {
            flush();
        }
        ssTables.values().forEach(table -> {
            try {
                table.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> cellIterator = cellIterator(ByteBuffer.allocate(0));
        final List<File> oldFiles = new ArrayList<>(generation);
        for (int i = 0; i < generation; i++) {
            final File dest = new File(storage, i + "_old" + SSTABLE_FILE_END);
            new File(storage, i + SSTABLE_FILE_END).renameTo(dest);
            oldFiles.add(dest);
        }
        generation = 0;
        final File fileTmp = new File(storage, generation + SSTABLE_TMP_FILE_END);
        SSTable.serialize(fileTmp, cellIterator);
        final File fileDst = new File(storage, generation + SSTABLE_FILE_END);
        final Path targetPath = fileDst.toPath();
        Files.move(fileTmp.toPath(), targetPath, StandardCopyOption.ATOMIC_MOVE);
        for (final File oldFile : oldFiles) {
            Files.delete(oldFile.toPath());
        }
        memTable.close();
        ssTables.clear();
        ssTables.put(generation, new SSTable(targetPath));
        generation++;
    }

    /**
     * Saving data on the disk.
     */
    public void flush() throws IOException {
        final File file = new File(storage, generation + SSTABLE_TMP_FILE_END);
        SSTable.serialize(file, memTable.iterator(ByteBuffer.allocate(0)));

        final File dst = new File(storage, generation + SSTABLE_FILE_END);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        ssTables.put(generation, new SSTable(dst.toPath()));
        generation++;
        memTable.close();
    }

    private void storeDataFromFile(@NotNull final Path path) {
        try {
            final String fileName = path.getFileName().toString();
            final String fileGenerationStr = fileName.substring(0, fileName.indexOf(SSTABLE_FILE_END));
            final int fileGeneration = Integer.parseInt(fileGenerationStr);
            generation = Math.max(generation, fileGeneration);
            ssTables.put(fileGeneration, new SSTable(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from) {
        final List<Iterator<Cell>> iterators = new ArrayList<>(ssTables.size() + 1);
        iterators.add(memTable.iterator(from));
        ssTables.descendingMap().values().forEach(ssTable -> {
            try {
                iterators.add(ssTable.iterator(from));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        final Iterator<Cell> mergedCellIterator = Iterators.mergeSorted(iterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        return Iters.collapseEquals(mergedCellIterator, Cell::getKey);
    }
}
