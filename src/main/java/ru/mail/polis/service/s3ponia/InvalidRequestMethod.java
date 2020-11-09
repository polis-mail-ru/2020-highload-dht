package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("serial")
public class InvalidRequestMethod extends Exception {

    public InvalidRequestMethod(@NotNull final String s, @NotNull final Throwable cause) {
        super(s, cause);
    }

    public InvalidRequestMethod(@NotNull final String s) {
        super(s);
    }
}
