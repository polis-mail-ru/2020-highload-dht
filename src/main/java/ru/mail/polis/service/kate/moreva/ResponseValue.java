package ru.mail.polis.service.kate.moreva;

/**
 * Utility class to work with request results.
 * */
public class ResponseValue {
    private final String status;
    private final byte[] body;
    private final long timestamp;

    /**
     * Response value constructor.
     *
     * @param status - the status of response.
     * @param body - the response body.
     * @param timestamp - timestamp of the needed Value.
     * */
    public ResponseValue(final String status, final byte[] body, final long timestamp) {
        this.status = status;
        this.body = body.clone();
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public long getTimestamp() {
        return timestamp;
    }
}

