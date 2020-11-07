package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

public class InvalidRequestHeaderException extends Exception {

    public InvalidRequestHeaderException(@NotNull final String s, @NotNull final Throwable cause) {
        super(s, cause);
    }

    public InvalidRequestHeaderException(@NotNull final String s) {
        super(s);
    }
}