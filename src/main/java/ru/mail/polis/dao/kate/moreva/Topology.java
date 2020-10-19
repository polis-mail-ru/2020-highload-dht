package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {
    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    int size();

    boolean isMe(@NotNull N node);

    @NotNull
    N[] all();
}
