package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {

    @NotNull
    N primaryFor(@NotNull ByteBuffer nodeKey);

    boolean isSelfId(@NotNull N node);

    int getClusterSize();

    N[] getNodes();
}
