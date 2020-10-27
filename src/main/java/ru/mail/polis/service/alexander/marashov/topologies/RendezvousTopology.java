package ru.mail.polis.service.alexander.marashov.topologies;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class RendezvousTopology implements Topology<String> {

    @NotNull
    private final String[] nodes;
    @NotNull
    private final String local;

    /**
     * Approach to distribute user's keys according to rendezvous hashing.
     * An algorithm hashes the key and the node's identifier together
     * and then pick the one with the highest hash value.
     *
     * @param nodesSet - set with all the nodes in the cluster.
     * @param local    - local node identifier.
     */
    public RendezvousTopology(@NotNull final Set<String> nodesSet, @NotNull final String local) {
        assert !nodesSet.isEmpty();
        assert nodesSet.contains(local);

        this.local = local;
        this.nodes = new String[nodesSet.size()];
        nodesSet.toArray(this.nodes);
        Arrays.sort(this.nodes);
    }

    @Override
    public boolean isLocal(final String node) {
        return node.equals(local);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        return primariesFor(key, 1)[0];
    }

    @Override
    public String[] primariesFor(final ByteBuffer key, final int nodesCount) {
        final int size = this.size();
        assert 0 < nodesCount;
        assert nodesCount <= size;

        final int keyHashCode = key.hashCode();
        final Queue<NodeKeyPair> minQueue = new PriorityQueue<>(nodesCount);
        for (int i = 0; i < nodesCount; ++i) {
            minQueue.add(new NodeKeyPair(nodes[i], keyHashCode));
        }
        for (int i = nodesCount; i < size; ++i) {
            final NodeKeyPair next = new NodeKeyPair(nodes[i], keyHashCode);
            final NodeKeyPair minElement = minQueue.peek();
            assert minElement != null;

            if (next.compareTo(minElement) < 0) {
                minQueue.poll();
                minQueue.add(next);
            }
        }
        final String[] primaries = new String[nodesCount];
        for (int j = nodesCount - 1; j >= 0; --j) {
            primaries[j] = Objects.requireNonNull(minQueue.poll()).getNode();
        }
        return primaries;
    }

    @NotNull
    @Override
    public String[] all() {
        return nodes.clone();
    }

    @Override
    public int size() {
        return nodes.length;
    }

    private static class NodeKeyPair implements Comparable<NodeKeyPair> {

        private final int hashCode;
        @NotNull
        private final String node;

        /**
         * NodeKeyPair calculates the hash code of the node combined with the key's hash code.
         *
         * @param node        - string representing the node
         * @param keyHashCode - integer hash code of the key
         */
        public NodeKeyPair(@NotNull final String node, final int keyHashCode) {
            this.node = node;
            int tmpHashCode = keyHashCode;
            for (int i = node.length() - 1; i >= 0; --i) {
                tmpHashCode = 31 * tmpHashCode + node.charAt(i);
            }
            this.hashCode = tmpHashCode;
        }

        @NotNull
        public String getNode() {
            return this.node;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        /*
          The more the less (reverse order)
         */
        public int compareTo(@NotNull final NodeKeyPair another) {
            return -Integer.compare(this.hashCode, another.hashCode);
        }
    }
}
