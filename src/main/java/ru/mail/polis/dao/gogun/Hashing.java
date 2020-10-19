package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface Hashing<T> {

    boolean isMe(@NotNull T node);

    @NotNull
    T get(@NotNull ByteBuffer key);

    int size();

    void add(@NotNull T node);

    @NotNull
    Collection<T> all();
}
