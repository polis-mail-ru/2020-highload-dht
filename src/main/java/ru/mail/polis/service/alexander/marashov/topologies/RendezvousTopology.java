package ru.mail.polis.service.alexander.marashov.topologies;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class RendezvousTopology implements Topology<String> {

    @NotNull
    private final Collection<String> nodes;
    @NotNull
    private final String local;

    /**
     * Approach to distribute user's keys according to rendezvous hashing.
     *     An algorithm hashes the key and the node's identifier together
     *     and then pick the one with the highest hash value.
     * @param nodes - sorted set with all the nodes in the cluster.
     * @param local - local node identifier.
     */
    public RendezvousTopology(@NotNull final SortedSet<String> nodes, @NotNull final String local) {
        assert nodes.size() > 0;
        assert nodes.contains(local);

        this.nodes = nodes;
        this.local = local;
    }

    @Override
    public boolean isLocal(final String node) {
        return node.equals(local);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        final int keyHashCode = key.hashCode();
        final Stream<Map.Entry<Integer, String>> sortedStream = nodes.stream()
                .map((nodeString) -> {

                    int hashCode = keyHashCode;
                    for (int i = nodeString.length() - 1; i >= 0; --i) {
                        hashCode = 31 * hashCode + nodeString.charAt(i);
                    }

                    return Map.entry(hashCode & Integer.MAX_VALUE, nodeString);
                })
                .sorted(Map.Entry.comparingByKey());

        return sortedStream
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No node for key"))
                .getValue();
    }

    @NotNull
    @Override
    public SortedSet<String> all() {
        return new TreeSet<>(nodes);
    }

    @Override
    public int size() {
        return nodes.size();
    }
}
