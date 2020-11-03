package ru.mail.polis.util;

@SuppressWarnings("serial")
public class NotEnoughReplicasException extends Exception{

    public NotEnoughReplicasException(final String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        synchronized (this) {
            return this;
        }
    }
}
