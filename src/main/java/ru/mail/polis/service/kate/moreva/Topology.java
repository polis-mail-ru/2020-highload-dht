package ru.mail.polis.service.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public interface Topology<N> {
    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    @NotNull
    Set<N> primaryFor(@NotNull ByteBuffer key, @NotNull Replicas replicas);


    int size();

    boolean isMe(@NotNull N node);

    @NotNull
    List<N> all();
}
