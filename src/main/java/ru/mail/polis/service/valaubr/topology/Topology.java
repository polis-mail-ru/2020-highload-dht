package ru.mail.polis.service.valaubr.topology;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {
    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    boolean isMe(@NotNull final String node);

    @NotNull
    N[] all();

    int size();
}
