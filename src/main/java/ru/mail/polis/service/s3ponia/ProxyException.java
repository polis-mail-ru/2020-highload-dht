package ru.mail.polis.service.s3ponia;

@SuppressWarnings("serial")
public class ProxyException extends Exception {
    public ProxyException(final String s, final Throwable cause) {
        super(s, cause);
    }

    public ProxyException(final String s) {
        super(s);
    }
}
