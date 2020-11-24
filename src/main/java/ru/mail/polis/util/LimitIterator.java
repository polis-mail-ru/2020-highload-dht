package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LimitIterator<T> implements Iterator<T> {
    final Iterator<T> iterator;
    final long size;
    long count = 0;
    T next;

    private void goNext() {
        if (this.iterator.hasNext()) {
            next = this.iterator.next();
            ++count;
        } else {
            next = null;
        }

        if (next != null && count >= size) {
            next = null;
        }
    }

    /**
     * Constructs an iterator that end in end value over another iterator of T.
     * @param iterator range's start point
     * @param size values' count
     */
    public LimitIterator(@NotNull final Iterator<T> iterator,
                         final long size) {
        this.iterator = iterator;
        this.size = size;
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
