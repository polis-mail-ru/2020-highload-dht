package ru.mail.polis.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RangeIterator<T extends Comparable<T>> implements Iterator<T> {
    final Iterator<T> iterator;
    final T end;

    T next;

    private void goNext() {
        if (this.iterator.hasNext()) {
            next = this.iterator.next();
        } else {
            next = null;
        }

        if (next != null && next.compareTo(end) >= 0) {
            next = null;
        }
    }

    public RangeIterator(Iterator<T> iterator, T end) {
        this.iterator = iterator;
        this.end = end;
        goNext();
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Empty range");
        }
        final var curr = next;
        goNext();
        return curr;
    }
}
