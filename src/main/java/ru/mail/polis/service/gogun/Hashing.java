package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Hashing<T> {

    boolean isMe(@NotNull T node);

    @NotNull
    T get(@NotNull ByteBuffer key);

    int size();

    @NotNull
    T[] all();
}
