package ru.mail.polis.service.alexander.marashov.topologies;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
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
        assert nodesSet.size() > 0;
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
        final int keyHashCode = key.hashCode();
        final NodeKeyPair[] pairs = new NodeKeyPair[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            pairs[i] = new NodeKeyPair(nodes[i], keyHashCode);
        }
        Arrays.sort(pairs);
        return pairs[0].getNode();
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
         * @param node - string representing the node
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
        public int compareTo(@NotNull final NodeKeyPair another) {
            return Integer.compare(this.hashCode, another.hashCode);
        }
    }
}
