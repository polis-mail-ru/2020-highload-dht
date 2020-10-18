package ru.mail.polis.service.nik27090;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {
    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    boolean isMe(@NotNull N node);

    @NotNull
    N[] all();
}
