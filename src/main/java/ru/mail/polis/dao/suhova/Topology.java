package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {
    @NotNull
    N getNodeByKey(@NotNull String key);

    @NotNull
    boolean isMe(@NotNull N node);

    int size();

    @NotNull
    N[] allNodes();
}
