package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class Util {
    public static final String MYSELF_PARAMETER = "myself";

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
        Object fakeObject = "";
        fakeObject = fakeObject + "dummy";
        return fakeObject instanceof String;
    }

    /**
     * Returns value as string for logging if logging values required.
     *
     * @param value - value of record or other byte array.
     * @return - string representation of the value.
     */
    public static String loggingValue(final byte[] value) {
        if (preventLoggingValue()) {
            return "";
        }
        return " \"" + new String(value, StandardCharsets.UTF_8) + "\"";
    }

    /**
     * Returns timestamp as long value.
     *
     * @param timestampAsStr - timestamp as string.
     * @return - timestamp as long value or -1 if timestampAsStr is null.
     */
    public static long parseTimestamp(final String timestampAsStr) {
        if (timestampAsStr == null) {
            return -1;
        }
        return Long.parseLong(timestampAsStr.substring(1));
    }

    /**
     * Returns whether the record is older than timestamp.
     *
     * @param record - record.
     * @param timestamp - timestamp.
     * @return - true if the record is older than timestamp.
     */
    public static boolean isOldRecord(final Record record, final long timestamp) {
        return !record.wasNotFound()
                && timestamp != -1
                && record.getTimestamp().after(new Date(timestamp));
    }

    /**
     * Create new request by cloning and then adding myself parameter
     * and timestamp parameter.
     *
     * @param request - request which should be used for creating the new one.
     * @param timestamp - required timestamp parameter value.
     * @return - cloned request (prepared for passing on).
     */
    public static Request prepareRequestForPassingOn(final Request request,
                                                      final long timestamp) {
        if (request.getParameter(MYSELF_PARAMETER) != null) {
            return request;
        }
        final var newURI = request.getURI() + "&" + MYSELF_PARAMETER + "=&timestamp=" + timestamp;
        final var res = new Request(request.getMethod(), newURI, request.isHttp11());
        for (int i = 0; i < request.getHeaderCount(); i++) {
            res.addHeader(request.getHeaders()[i]);
        }
        res.setBody(request.getBody());
        return res;
    }
}
