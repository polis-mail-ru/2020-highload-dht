package ru.mail.polis.dao.alexander.marashov.iterators;

import ru.mail.polis.dao.alexander.marashov.Cell;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class CellIterator implements Iterator<Cell> {

    private final TableIteratorPriorityQueue tableIteratorPriorityQueue;

    /**
     * Creates an CellIterator instance from the list of table iterators.
     */
    public CellIterator(final List<TableIterator> tableIteratorList) {
        tableIteratorPriorityQueue = new TableIteratorPriorityQueue();
        tableIteratorPriorityQueue.addAll(tableIteratorList);
    }

    @Override
    public boolean hasNext() {
        return !tableIteratorPriorityQueue.isEmpty();
    }

    @Override
    public Cell next() {
        if (tableIteratorPriorityQueue.isEmpty()) {
            throw new NoSuchElementException("No more cells");
        }
        final TableIterator topTable = tableIteratorPriorityQueue.poll();
        final Cell result = topTable.getBufferedCell();
        while (!tableIteratorPriorityQueue.isEmpty()) {
            final Cell nextCell = tableIteratorPriorityQueue.peek().getBufferedCell();
            if (nextCell.getKey().compareTo(result.getKey()) != 0) {
                break;
            }
            final TableIterator tableIterator = tableIteratorPriorityQueue.poll();
            tableIterator.next();
            tableIteratorPriorityQueue.add(tableIterator);
        }
        topTable.next();
        tableIteratorPriorityQueue.add(topTable);
        return result;
    }
}
