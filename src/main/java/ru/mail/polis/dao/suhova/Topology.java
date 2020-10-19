package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;

public interface Topology<N> {
    @NotNull
    N getNodeByKey(@NotNull final String key);

    @NotNull
    boolean isMe(@NotNull N node);

    int size();

    @NotNull
    N[] allNodes();
}
