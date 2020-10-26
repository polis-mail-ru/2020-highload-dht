package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Util {
    /**
     * Converts any collection to new sorted list.
     *
     * @param c - collection.
     * @param <T> - type of element.
     * @return - sorted list.
     */
    public static
    <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }
}
