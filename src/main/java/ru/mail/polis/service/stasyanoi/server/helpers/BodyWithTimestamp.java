package ru.mail.polis.service.stasyanoi.server.helpers;

import com.google.common.primitives.Longs;
import ru.mail.polis.service.stasyanoi.Constants;

import java.util.Arrays;

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
        final int timestampLength = Longs.toByteArray(System.currentTimeMillis()).length;
        final byte[] timestampTemp = new byte[timestampLength];
        final int realBodyLength = body.length - timestampLength;
        System.arraycopy(body, realBodyLength, timestampTemp, 0, timestampTemp.length);
        final byte[] newBody = new byte[realBodyLength];
        System.arraycopy(body, 0, newBody, 0, newBody.length);
        if (body.length == Constants.EMPTY_BODY_SIZE) {
            this.pureBody = new byte[0];
        } else {
            this.pureBody = newBody;
        }
        this.timestamp = timestampTemp;
    }

    public byte[] getPureBody() {
        return Arrays.copyOf(pureBody, pureBody.length);
    }

    public byte[] getTimestamp() {
        return Arrays.copyOf(timestamp, timestamp.length);
    }
}
