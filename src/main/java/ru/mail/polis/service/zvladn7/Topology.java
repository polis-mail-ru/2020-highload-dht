package ru.mail.polis.service.zvladn7;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Topology to represent the node of cluster.
 * @param <N> - identifier of node
 */
public interface Topology<N> {

    @NotNull
    N nodeFor(@NotNull ByteBuffer key);

    @NotNull
    Set<N> nodesForKey(@NotNull ByteBuffer key, int from);

    boolean isLocal(@NotNull String node);

    boolean isLocal(@NotNull Set<String> nodes);

    int size();

    N[] nodes();

    N local();

}
