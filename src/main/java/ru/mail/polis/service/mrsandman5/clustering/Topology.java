package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull final ByteBuffer key);

    boolean isNotMe(@NotNull final T node);

    @NotNull
    Set<T> all();

    default Set<T> others() {
        return all().stream()
                .filter(this::isNotMe)
                .collect(Collectors.toSet());
    }
}
