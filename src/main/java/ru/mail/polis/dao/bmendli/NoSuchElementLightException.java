package ru.mail.polis.dao.bmendli;

import java.util.NoSuchElementException;

public class NoSuchElementLightException extends NoSuchElementException {
    private static final long serialVersionUID = 6769829250639411880L;

    public NoSuchElementLightException(final String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
