package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull final ByteBuffer key);

    @NotNull
    Set<T> replicasFor(@NotNull final ByteBuffer key,
                       @NotNull final ReplicasFactor replicasFactor);

    boolean isMe(@NotNull final T node);

    @NotNull
    Set<T> all();

    default boolean isNotMe(@NotNull final T node) {
        return !isMe(node);
    }

    /** Find all nodes, which doesn't belong to topology.
     */
    default Set<T> others() {
        return all().stream()
                .filter(this::isNotMe)
                .collect(Collectors.toSet());
    }
}
