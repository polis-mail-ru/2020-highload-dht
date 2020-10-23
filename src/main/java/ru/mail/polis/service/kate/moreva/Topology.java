package ru.mail.polis.service.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;

public interface Topology<N> {
    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    int size();

    boolean isMe(@NotNull N node);

    @NotNull
    List<N> all();
}
