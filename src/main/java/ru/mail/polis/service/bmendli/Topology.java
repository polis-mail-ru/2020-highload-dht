package ru.mail.polis.service.bmendli;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull final ByteBuffer key);

    @NotNull
    T[] all();

    int size();

    boolean isLocal(@NotNull final T node);
}
