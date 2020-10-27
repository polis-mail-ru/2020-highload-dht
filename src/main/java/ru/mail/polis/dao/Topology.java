package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

public interface Topology<T> {
    @NotNull
    T[] getNodeByKey(@NotNull String key, final int length);

    @NotNull
    boolean equalsUrl(@NotNull T node);

    int getSize();

    @NotNull
    T[] getAllNodes();
}
