package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BasicTopology<T> implements Topology<T> {

    private final T me;
    private final List<T> nodes;

    /** Basic cluster topology.
     * @param topology - nodes of the cluster
     * @param me       - name of the current node
     */
    public BasicTopology(@NotNull final Set<T> topology,
                         @NotNull final T me) {
        this.me = me;
        this.nodes = topology.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public T primaryFor(@NotNull final ByteBuffer key) {
        return nodes.get(getHash(key));
    }

    @NotNull
    @Override
    public Set<T> replicasFor(@NotNull final ByteBuffer key,
                              @NotNull final ReplicasFactor replicasFactor) {
        if (replicasFactor.getFrom() > nodes.size()) {
            throw new IllegalArgumentException("Number of nodes is bigger than from!");
        }

        final Set<T> result = new HashSet<>();
        int startIndex = getHash(key);
        while (result.size() < replicasFactor.getFrom()) {
            result.add(nodes.get(startIndex));
            startIndex++;
            if (startIndex == nodes.size()) {
                startIndex = 0;
            }
        }
        return result;
    }

    @Override
    public boolean isNotMe(@NotNull final T node) {
        return !me.equals(node);
    }

    @NotNull
    @Override
    public Set<T> all() {
        return new HashSet<>(nodes);
    }

    private int getHash(@NotNull final ByteBuffer key) {
        final int hash = key.hashCode();
        return (hash & Integer.MAX_VALUE) % nodes.size();
    }
}
