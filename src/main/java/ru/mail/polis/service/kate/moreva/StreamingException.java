package ru.mail.polis.service.kate.moreva;

import java.io.IOException;

public class StreamingException extends IOException {

    private static final long serialVersionUID = Long.MAX_VALUE;

    public StreamingException(final Exception message) {
        super(message);
    }

    public StreamingException(final String message) {
        super(message);
    }
}
