package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<I> {
    @NotNull
    I identifyByKey(@NotNull byte[] key);

    @NotNull
    I[] replicasFor(@NotNull ByteBuffer key, int replicas);

    int nodeCount();

    @NotNull
    I[] allNodes();

    boolean isMe(I node);

    I getCurrentNode();
}
