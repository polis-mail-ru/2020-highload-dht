package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("serial")
public class FutureResponseException extends RuntimeException {

    public FutureResponseException(@NotNull final String s, @NotNull final Throwable cause) {
        super(s, cause);
    }

    public FutureResponseException(@NotNull final String s) {
        super(s);
    }
}

