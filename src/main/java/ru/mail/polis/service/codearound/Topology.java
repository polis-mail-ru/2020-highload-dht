package ru.mail.polis.service.codearound;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Set;

@ThreadSafe
public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull ByteBuffer key);

    boolean isThisNode(@NotNull T nodeId);

    @NotNull
    Set<T> getNodes();

    @NotNull
    String[] replicasFor(@NotNull final ByteBuffer id, final int numOfReplicas);

    @NotNull
    String getThisNode();

    int getClusterSize();
}
