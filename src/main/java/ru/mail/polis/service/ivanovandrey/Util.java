package ru.mail.polis.service.ivanovandrey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Util {
    private Util() {
        /* nothing */
    }

    /**
     * Converts any collection to new sorted list.
     *
     * @param c - collection.
     * @param <T> - type of element.
     * @return - sorted list.
     */
    public static
    <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
        final List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }
}
