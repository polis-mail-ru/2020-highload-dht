package ru.mail.polis.service.kate.moreva;

import java.util.Arrays;

public class RequestValue {
    private final String status;
    private final byte[] body;
    private final long timestamp;

    public RequestValue(final String status, final byte[] body, final long timestamp) {
        this.status = status;
        this.body = Arrays.copyOf(body, body.length);
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public byte[] getValue() {
        return Arrays.copyOf(body, body.length);
    }

    public long getTimestamp() {
        return timestamp;
    }
}

