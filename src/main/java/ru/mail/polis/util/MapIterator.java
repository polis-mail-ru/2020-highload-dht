package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public class MapIterator<T, U> implements Iterator<U> {
    final Iterator<T> iterator;
    final Function<T, U> mapFunction;

    public MapIterator(@NotNull final Iterator<T> iterator,
                       @NotNull final Function<T, U> mapFunction) {
        this.iterator = iterator;
        this.mapFunction = mapFunction;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public U next() {
        return mapFunction.apply(iterator.next());
    }
}
