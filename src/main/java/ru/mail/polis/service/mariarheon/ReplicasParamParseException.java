package ru.mail.polis.service.mariarheon;

public class ReplicasParamParseException extends Exception {
    private static final long serialVersionUID = 0x8FA823913BC8E188L;

    public ReplicasParamParseException(final String msg) {
        super(msg);
    }

    public ReplicasParamParseException(final String msg, final Throwable throwable) {
        super(msg, throwable);
    }
}
