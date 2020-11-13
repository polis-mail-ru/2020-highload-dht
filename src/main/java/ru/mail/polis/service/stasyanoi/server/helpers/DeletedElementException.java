package ru.mail.polis.service.stasyanoi.server.helpers;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class DeletedElementException extends NoSuchElementException {

    private static final long serialVersionUID = 127348716983473L;

    private final byte[] timestamp;

    public DeletedElementException(final String msg, final byte[] timestamp) {
        super(msg);
        this.timestamp = Arrays.copyOf(timestamp, timestamp.length);
    }

    public byte[] getTimestamp() {
        return Arrays.copyOf(timestamp, timestamp.length);
    }
}
