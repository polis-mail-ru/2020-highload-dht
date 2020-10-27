package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;

public interface Hashing<T> {

    boolean isMe(@NotNull T node);

    @NotNull
    T get(@NotNull ByteBuffer key);

    int size();

    @NotNull
    T[] all();

    @NotNull
    List<T> getReplNodes(@NotNull T node, int count);
}
