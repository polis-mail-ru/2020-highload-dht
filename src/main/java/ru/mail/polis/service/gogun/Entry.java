package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

final class Entry {
    public static final long ABSENT = -1;
    public static final byte[] EMPTY_DATA = new byte[0];
    private final byte[] body;
    private Status status;
    private long timestamp;

    public Entry(final long timestamp,
                 final byte[] body,
                 final Status status) {
        this.timestamp = timestamp;
        this.body = body.clone();
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
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
}
