package ru.mail.polis.service.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {

    @NotNull
    N primaryFor(@NotNull ByteBuffer key);

    boolean isMyNode(@NotNull N node);

    int sizeOfAllNodesInCluster();

    @NotNull
    N[] all();
}
