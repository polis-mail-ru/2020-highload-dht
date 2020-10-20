package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

public interface Topology<Node> {
    @NotNull
    Node getNodeByKey(@NotNull String key);

    @NotNull
    boolean equalsUrl(@NotNull Node node);

    int getSize();

    @NotNull
    Node[] getAllNodes();
}
