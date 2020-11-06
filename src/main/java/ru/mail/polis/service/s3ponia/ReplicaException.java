package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("serial")
public class ReplicaException extends Throwable {

    public ReplicaException(@NotNull final String s,
                     final Throwable cause) {
        super(s, cause);
    }

    public ReplicaException(@NotNull final String s) {
        super(s);
    }
}
