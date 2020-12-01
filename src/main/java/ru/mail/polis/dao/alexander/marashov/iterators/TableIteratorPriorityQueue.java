package ru.mail.polis.dao.alexander.marashov.iterators;

import java.util.PriorityQueue;

/**
 * Priority queue for TableIterators.
 * If the tableIterator is empty the queue deletes it.
 */
public class TableIteratorPriorityQueue extends PriorityQueue<TableIterator> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean add(final TableIterator tableIterator) {
        if (tableIterator.getBufferedCell() == null) {
            return false;
        } else {
            return super.add(tableIterator);
        }
    }
}
