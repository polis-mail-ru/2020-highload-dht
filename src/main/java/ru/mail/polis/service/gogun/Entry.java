package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

final class Entry {
    public static final long EMPTY_TIMESTAMP = -1;
    public static final byte[] EMPTY_DATA = new byte[0];
    private final byte[] body;
    private Status status;
    private final long timestamp;

    private Entry(final long timestamp,
                 final byte[] body,
                 final Status status) {
        this.timestamp = timestamp;
        this.body = body.clone();
        this.status = status;
    }

    public static Entry absent() {
        return new Entry(Entry.EMPTY_TIMESTAMP, Entry.EMPTY_DATA, Status.ABSENT);
    }

    public static Entry removed(final long timestamp) {
        return new Entry(timestamp, Entry.EMPTY_DATA, Status.REMOVED);
    }

    public static Entry present(final long timestamp, final byte[] data) {
        return new Entry(timestamp, data, Status.PRESENT);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public long getTimestamp() {
        return timestamp;
    }

    static Response toProxyResponse(@NotNull final Entry value) {
        Response response;
        switch (value.status) {
            case PRESENT:
                response = Response.ok(value.body);
                response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + value.timestamp);
                return response;
            case REMOVED:
                response = new Response(Response.NOT_FOUND, Response.EMPTY);
                response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + value.timestamp);
                return response;
            case ABSENT:
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            default:
                throw new IllegalStateException("Unknown value response value state");
        }
    }

    public enum Status {
        PRESENT,
        ABSENT,
        REMOVED
    }
}
