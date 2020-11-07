package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

public class InvalidRequestParametersException extends Exception {

    public InvalidRequestParametersException(@NotNull final String s, @NotNull final Throwable cause) {
        super(s, cause);
    }

    public InvalidRequestParametersException(@NotNull final String s) {
        super(s);
    }
}
