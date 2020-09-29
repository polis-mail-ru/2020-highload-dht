package ru.mail.polis.dao.dmrnz;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.Record;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public final class LSMDao implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";
    private static final String PREFIX = "SSTABLE";
    private static final ByteBuffer MAX_REACHABLE_VALUE = ByteBuffer.allocate(0);

    private Table memTable = new MemTable();
    private final File base;
    private int generation;
    private final long flushThreshold;
    private final Collection<SSTable> ssTables;

    /**
     * Creates LSM Dao.
     *
     * @param base           is directory with SSTables
     * @param flushThreshold is threshold of MemTable's size
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    public LSMDao(@NotNull final File base, final long flushThreshold) throws IOException {
        this.base = base;
        assert flushThreshold >= 0L;
        this.flushThreshold = flushThreshold;
        ssTables = new ArrayList<>();

        final var options = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        final int maxDepth = 1;

        Files.walkFileTree(base.toPath(), options, maxDepth, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
                if (path.getFileName().toString().endsWith(SUFFIX)
                        && path.getFileName().toString().startsWith(PREFIX)) {
                    ssTables.add(new SSTable(path.toFile()));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        generation = ssTables.size() + 1;
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(cellIterator(from, true),
                cell -> Record.of(cell.getKey(), cell.getValue().getData()));
    }

    @NotNull
    private Iterator<Cell> cellIterator(@NotNull final ByteBuffer from, final boolean isDirect) throws IOException {

        final List<Iterator<Cell>> ssTablesIterator = new ArrayList<>();

        if (isDirect) {
            for (final SSTable ssTable : ssTables) {
                ssTablesIterator.add(ssTable.iterator(from));
            }
            ssTablesIterator.add(memTable.iterator(from));
        } else {
            if (isUnreachableLastKey(from)) {
                for (final SSTable ssTable : ssTables) {
                    ssTablesIterator.add(ssTable.reverseIterator());
                }
                ssTablesIterator.add(memTable.reverseIterator());
            } else {
                for (final SSTable ssTable : ssTables) {
                    ssTablesIterator.add(ssTable.reverseIterator(from));
                }
                ssTablesIterator.add(memTable.reverseIterator(from));
            }
        }

        final Iterator<Cell> mergedCells = Iterators.mergeSorted(ssTablesIterator, Cell.COMPARATOR);
        final Iterator<Cell> cells = Iters.collapseEquals(mergedCells, Cell::getKey);

        return Iterators.filter(cells, cell -> !cell.getValue().isRemoved());
    }

    @SuppressWarnings("ReferenceEquality")
    private boolean isUnreachableLastKey(@NotNull final ByteBuffer from) {
        return from == MAX_REACHABLE_VALUE;
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value) throws IOException {
        memTable.upsert(key, value);
        if (memTable.sizeInBytes() >= flushThreshold) flush();
    }

    private void flush() throws IOException {
        final File tmp = new File(base, PREFIX + generation + TEMP);
        SSTable.write(memTable.iterator(ByteBuffer.allocate(0)), tmp);

        final File dest = new File(base, PREFIX + generation + SUFFIX);
        Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.add(new SSTable(dest));

        generation++;
        memTable = new MemTable();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        if (memTable.sizeInBytes() >= flushThreshold) flush();
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> cells = cellIterator(ByteBuffer.allocate(0), true);
        final File temp = new File(base, PREFIX + 1 + TEMP);
        SSTable.write(cells, temp);

        for (final SSTable ssTable : ssTables) {
            ssTable.deleteSSTableFile();
        }

        ssTables.clear();

        final File dest = new File(base, PREFIX + 1 + SUFFIX);
        Files.move(temp.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
        ssTables.add(new SSTable(dest));

        generation = ssTables.size() + 1;
    }

    @Override
    public void close() throws IOException {
        if (memTable.sizeInBytes() != 0) {
            flush();
        }

        for (final SSTable ssTable : ssTables) {
            ssTable.close();
        }
    }
}
