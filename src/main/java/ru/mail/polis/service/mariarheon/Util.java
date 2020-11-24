package ru.mail.polis.service.mariarheon;

import java.nio.charset.StandardCharsets;
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
     * Compare two arrays of bytes as unsigned bytes lexicographically.
     *
     * @param left - first byte array
     * @param right - second byte array
     * @return 0 if equal, 1 if left more than right, -1 otherwise.
     */
    public static int compare(final byte[] left, final byte[] right) {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            final int a = (left[i] & 0xff);
            final int b = (right[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }

    private static boolean preventLoggingValue() {
        return true;
    }

    public static String loggingValue(final byte[] value) {
        if (preventLoggingValue()) {
            return "";
        }
        return " \"" + new String(value, StandardCharsets.UTF_8) + "\"";
    }
}
