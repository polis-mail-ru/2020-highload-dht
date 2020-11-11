package ru.mail.polis.service.stasyanoi.server.helpers;

import java.util.NoSuchElementException;

public class DeletedElementException extends NoSuchElementException {

    private static final long serialVersionUID = 127348716983473L;

    private final byte[] timestamp;

    public DeletedElementException(final String msg, final byte[] timestamp) {
        super(msg);
        this.timestamp = timestamp;
    }

    public byte[] getTimestamp() {
        return timestamp;
    }
}
