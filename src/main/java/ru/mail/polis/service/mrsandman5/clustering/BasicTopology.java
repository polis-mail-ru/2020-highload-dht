package ru.mail.polis.service.mrsandman5.clustering;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BasicTopology implements Topology<String> {

    private final String me;
    private final List<String> nodes;

    /**
     * Basic cluster topology.
     *
     * @param topology nodes of the cluster
     * @param me       name of the current node
     */
    public BasicTopology(
            @NotNull final Set<String> topology,
            @NotNull final String me) {
        this.me = me;
        this.nodes = topology.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        final int hash = key.hashCode();
        final int index = (hash & Integer.MAX_VALUE) % nodes.size();
        return nodes.get(index);
    }

    @Override
    public boolean isNotMe(@NotNull final String node) {
        return !me.equals(node);
    }

    @NotNull
    @Override
    public Collection<String> all() {
        return nodes;
    }
}
