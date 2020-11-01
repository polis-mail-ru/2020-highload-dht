package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;

public interface Topology<N> {
    @NotNull
    N[] getNodesByKey(@NotNull final String key, final int n);

    @NotNull
    boolean isMe(@NotNull N node);

    int size();

    int quorumSize();

    @NotNull
    N[] allNodes();
}
