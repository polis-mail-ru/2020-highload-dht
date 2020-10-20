package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BasicTopology<T> implements Topology<T> {

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
        final int hash = key.hashCode();
        final int index = (hash & Integer.MAX_VALUE) % nodes.size();
        return nodes.get(index);
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

}
