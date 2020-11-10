package ru.mail.polis.service.stasyanoi.server.helpers;

/**
 * Body with timestamp object.
 */
public class BodyWithTimestamp {

    private final byte[] pureBody;
    private final byte[] timestamp;

    /**
     * Create body with timestamp object.
     *
     * @param body - body with timestamp.
     */
    public BodyWithTimestamp(final byte[] body) {
        final int length = String.valueOf(System.nanoTime()).length();
        final byte[] timestamp = new byte[length];
        final int realBodyLength = body.length - length;
        System.arraycopy(body, realBodyLength, timestamp, 0, timestamp.length);
        final byte[] newBody = new byte[realBodyLength];
        System.arraycopy(body, 0, newBody, 0, newBody.length);
        this.pureBody = newBody;
        this.timestamp = timestamp;
    }

    public byte[] getPureBody() {
        return pureBody;
    }

    public byte[] getTimestamp() {
        return timestamp;
    }
}
