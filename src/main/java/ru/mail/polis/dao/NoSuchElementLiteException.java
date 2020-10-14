package ru.mail.polis.dao;

import java.util.NoSuchElementException;

@SuppressWarnings("serial")
public class NoSuchElementLiteException extends NoSuchElementException {

    NoSuchElementLiteException(final String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        synchronized (this) {
            return this;
        }
    }
}
