package ru.mail.polis.dao.alexander.marashov.iterators;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.alexander.marashov.Cell;
import ru.mail.polis.dao.alexander.marashov.Table;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Class for access to Table data with buffered Cell value.
 * TableIterator is compared first by Cell, then by generation in descending order.
 */
public final class TableIterator implements Comparable<TableIterator> {

    private final Integer generation;
    private final Iterator<Cell> cellIterator;
    private Cell bufferedCell;

    /**
     * Creates an TableIterator instance.
     */
    public TableIterator(
            final Integer generation,
            final Table ssTable
    ) throws IOException {
        this.generation = generation;
        this.cellIterator = ssTable.iterator(ByteBuffer.allocate(0));
        this.bufferedCell = cellIterator.hasNext() ? cellIterator.next() : null;
    }

    public void next() {
        bufferedCell = cellIterator.hasNext() ? cellIterator.next() : null;
    }

    public Cell getBufferedCell() {
        return bufferedCell;
    }

    @Override
    public int compareTo(@NotNull final TableIterator o) {
        if (this == o || (bufferedCell == null && o.getBufferedCell() == null)) {
            return 0;
        }
        if (bufferedCell == null) {
            return 1;
        } else if (o.getBufferedCell() == null) {
            return -1;
        }
        final int comp = Cell.COMPARATOR.compare(this.getBufferedCell(), o.getBufferedCell());
        if (comp == 0) {
            return -this.generation.compareTo(o.generation);
        } else {
            return comp;
        }
    }
}
