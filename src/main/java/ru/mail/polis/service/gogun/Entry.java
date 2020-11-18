package ru.mail.polis.service.gogun;

import one.nio.http.Response;

final class Entry {
    public static final long ABSENT = -1;
    public static final byte[] EMPTY_DATA = new byte[0];
    public static final int CREATED = 201;
    public static final int OK = 200;
    public static final int ACCEPTED = 202;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    private final byte[] body;

    private int status;

    private long timestamp;

    public Entry(final long timestamp,
                 final byte[] body,
                 final int status) {
        this.timestamp = timestamp;
        this.body = body;
        this.status = status;
    }

    public Entry() {
        this.timestamp = ABSENT;
        this.body = EMPTY_DATA;
        this.status = 0;
    }

    public Entry(final byte[] body,
                 final int status) {
        this.timestamp = Entry.ABSENT;
        this.body = body;
        this.status = status;
    }

    public Entry(final int status) {
        this.timestamp = Entry.ABSENT;
        this.body = EMPTY_DATA;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getBody() {
        return body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Response getResponse() {
        Response response = new Response(getResponseCode(status), body);
        if (timestamp != Entry.ABSENT) {
            response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + timestamp);
        }

        return response;
    }

    private String getResponseCode(int status) {
        switch (status) {
            case Entry.OK:
                return Response.OK;
            case Entry.CREATED:
                return Response.CREATED;
            case Entry.NOT_FOUND:
                return Response.NOT_FOUND;
            case Entry.INTERNAL_ERROR:
                return Response.INTERNAL_ERROR;
            case Entry.ACCEPTED:
                return Response.ACCEPTED;
            default:
                return null;
        }
    }

}
