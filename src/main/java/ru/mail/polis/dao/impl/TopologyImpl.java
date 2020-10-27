package ru.mail.polis.dao.impl;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.Topology;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public class TopologyImpl implements Topology<String> {
    @NotNull
    private final String[] topology;
    @NotNull
    private final String url;

    /**
     * Implementation Topology interface.
     *
     * @param topology - topology
     * @param url      - node url
     */
    public TopologyImpl(
            @NotNull final Set<String> topology,
            @NotNull final String url) {
        assert topology.contains(url);
        this.url = url;
        this.topology = new String[topology.size()];
        topology.toArray(this.topology);
    }

    @NotNull
    @Override
    public String[] getNodeByKey(@NotNull final String key, final int length) {
        if (length == getSize()) {
            return topology.clone();
        }
        return Arrays.stream(topology)
                .sorted(Comparator.comparingInt(node -> (node + key).hashCode()))
                .limit(length)
                .toArray(String[]::new);
    }

    @NotNull
    @Override
    public boolean equalsUrl(@NotNull final String node) {
        return node.equals(url);
    }

    @Override
    public int getSize() {
        return topology.length;
    }

    @NotNull
    @Override
    public String[] getAllNodes() {
        return topology.clone();
    }
}
