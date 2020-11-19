package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import static com.google.common.hash.Hashing.murmur3_128;

public class RendezvousTopology implements Topology<String> {
    @NotNull
    private final String[] topology;
    @NotNull
    private final String me;

    /**
     * Implementation {@link Topology}.
     *
     * @param topology - nodes
     * @param me       - current node
     */
    public RendezvousTopology(@NotNull final Set<String> topology,
                              @NotNull final String me) {
        assert topology.contains(me);
        this.me = me;
        this.topology = new String[topology.size()];
        topology.toArray(this.topology);
    }

    @NotNull
    @Override
    public String[] getNodesByKey(@NotNull final String key, final int n) {
        if (n == size()) return topology.clone();
        return Arrays.stream(topology)
            .sorted(Comparator.comparingInt(node ->
                murmur3_128().newHasher().putUnencodedChars(key + node).hash().hashCode()))
            .limit(n)
            .toArray(String[]::new);
    }

    @NotNull
    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    @Override
    public int size() {
        return topology.length;
    }

    @Override
    public int quorumSize() {
        return topology.length / 2 + 1;
    }

    @NotNull
    @Override
    public String[] allNodes() {
        return topology.clone();
    }
}
