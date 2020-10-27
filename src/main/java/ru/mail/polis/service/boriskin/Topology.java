package ru.mail.polis.service.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public interface Topology<N> {

    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    boolean isMyNode(@NotNull N node);

    @NotNull
    Set<N> all();

    @NotNull
    List<N> replicas(@NotNull ByteBuffer key, int from);

    @NotNull
    N recogniseMyself();
}
