package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public interface Hashing<T> {

    boolean isMe(@NotNull T node);

    int size();

    @NotNull
    List<T> all();

    @NotNull
    Set<T> primaryFor(@NotNull ByteBuffer key, int count);
}
