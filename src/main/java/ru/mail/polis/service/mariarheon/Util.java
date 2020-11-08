package ru.mail.polis.service.mariarheon;

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

    /**
     * Concatenate arrays of bytes.
     *
     * @param a - first array of bytes.
     * @param b - second array of bytes.
     * @return - result array of bytes.
     */
    public byte[] concatenate(byte[] a, byte[] b) {
        final int aLen = a.length;
        final int bLen = b.length;

        final byte[] res = new byte[aLen + bLen];
        System.arraycopy(a, 0, res, 0, aLen);
        System.arraycopy(b, 0, res, aLen, bLen);

        return res;
    }
}
