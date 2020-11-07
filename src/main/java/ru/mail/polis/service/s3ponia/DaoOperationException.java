package ru.mail.polis.service.s3ponia;

@SuppressWarnings("serial")
public class DaoOperationException extends Exception {

    public DaoOperationException(final String s, final Throwable cause) {
        super(s, cause);
    }

    public DaoOperationException(final String s) {
        super(s);
    }
}
