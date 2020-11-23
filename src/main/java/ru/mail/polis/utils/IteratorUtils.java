package ru.mail.polis.utils;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.tables.TableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class IteratorUtils {

    private IteratorUtils() {
    }

    /**
     * Create an iterator over alive {@link Cell}.
     * @param from data on which iterator is created
     * @return an iterator over alive cells
     */
    @NotNull
    public static Iterator<Cell> cellIterator(@NotNull final ByteBuffer from,
                                              @NotNull final DAOImpl dao) throws IOException {
        final TableSet snapshot;
        dao.getLock().readLock().lock();
        try {
            snapshot = dao.getTableSet();
        } finally {
            dao.getLock().readLock().unlock();
        }
        final List<Iterator<Cell>> fileIterators = new ArrayList<>(snapshot.ssTables.size() + 1);
        fileIterators.add(snapshot.memTable.iterator(from));
        snapshot.flushing.forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        final Iterator<Cell> fresh = freshCellIterators(snapshot, from, fileIterators);
        return Iterators.filter(
                fresh, cell -> !Objects.requireNonNull(cell).getValue().isTombstone() &&
                        !Objects.requireNonNull(cell).getValue().isExpired());
    }

    /**
     * Create an iterator over fresh {@link Cell}.
     * @param from data on which iterator is created
     * @return an iterator over fresh cells
     */
    @SuppressWarnings("UnstableApiUsage")
    public static Iterator<Cell> freshCellIterators(@NotNull final TableSet snapshot,
                                                    @NotNull final ByteBuffer from,
                                                    @NotNull final List<Iterator<Cell>> fileIterators) {
        snapshot.ssTables.descendingMap().values().forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        final Iterator<Cell> merged = Iterators.mergeSorted(fileIterators, Comparator.naturalOrder());
        return Iters.collapseEquals(merged, Cell::getKey);
    }

    /**
     * Create an iterator over entries {@link Cell} (replication case).
     * @param from data on which iterator is created
     * @return an iterator over entries
     */
    @SuppressWarnings("UnstableApiUsage")
    public static Iterator<Cell> entryIterators(@NotNull final ByteBuffer from,
                                                @NotNull final DAOImpl dao) throws IOException {
        final TableSet current = dao.getTableSet();
        final List<Iterator<Cell>> fileIterators =
                new ArrayList<>(dao.getTableSet().ssTables.size() + 1);
        current.ssTables.descendingMap().values().forEach(v -> {
            try {
                fileIterators.add(v.iterator(from));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        fileIterators.add(current.memTable.iterator(from));
        final Iterator<Cell> mergedCells = Iterators.mergeSorted(fileIterators, Comparator.naturalOrder());
        return Iters.collapseEquals(mergedCells, Cell::getKey);
    }

}
