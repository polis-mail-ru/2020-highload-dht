package ru.mail.polis.dao.alexander.marashov;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

public final class CellIterator implements Iterator<Cell> {

    private final PriorityQueue<TableIterator> tableIteratorPriorityQueue;

    /**
     * Creates an CellIterator instance from the list of table iterators.
     */
    public CellIterator(final List<TableIterator> tableIteratorList) {
        tableIteratorPriorityQueue = new PriorityQueue<>() {
            @Override
            public boolean add(final TableIterator tableIterator) {
                if (tableIterator.getBufferedCell() == null) {
                    return false;
                } else {
                    return super.add(tableIterator);
                }
            }
        };
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
