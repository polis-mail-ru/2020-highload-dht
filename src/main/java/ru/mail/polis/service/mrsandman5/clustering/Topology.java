package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull final ByteBuffer key);

    boolean isNotMe(@NotNull final T node);

    @NotNull
    Collection<T> all();
}
