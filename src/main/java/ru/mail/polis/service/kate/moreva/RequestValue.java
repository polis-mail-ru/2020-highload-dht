package ru.mail.polis.service.kate.moreva;

import java.util.Arrays;

/**
 * Utility class to work with request results.
 * */
public class RequestValue {
    private final String status;
    private final byte[] body;
    private final long timestamp;

    public RequestValue(final String status, final byte[] body, final long timestamp) {
        this.status = status;
        this.body = body.clone();
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public byte[] getValue() {
        return body.clone();
    }

    public long getTimestamp() {
        return timestamp;
    }
}

