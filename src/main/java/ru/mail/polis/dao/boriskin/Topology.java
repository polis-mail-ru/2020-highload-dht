package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<Node> {

    @NotNull
    Node primaryFor(@NotNull ByteBuffer key);

    boolean isMyNode(@NotNull Node node);

    int sizeOfAllNodesInCluster();

    @NotNull
    Node[] all();
}
