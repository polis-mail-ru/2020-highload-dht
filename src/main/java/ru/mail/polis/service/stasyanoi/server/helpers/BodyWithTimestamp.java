package ru.mail.polis.service.stasyanoi.server.helpers;

import java.util.Arrays;

/**
 * Body with timestamp object.
 */
public class BodyWithTimestamp {

    private final byte[] pureBody;
    private final byte[] timestampObj;

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
        this.timestampObj = timestamp;
    }

    public byte[] getPureBody() {
        return Arrays.copyOf(pureBody, pureBody.length);
    }

    public byte[] getTimestampObj() {
        return Arrays.copyOf(timestampObj, timestampObj.length);
    }
}
