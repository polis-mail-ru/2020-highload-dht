package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.hash.Hashing.goodFastHash;

public class RendezvousTopology implements Topology<String> {

    private final List<String> nodes;
    @NotNull
    private final String localNode;

    /**
     * Initialization and preparation of nodes.
     *
     * @param nodes     - cluster nodes.
     * @param localNode - our node.
     */

    public RendezvousTopology(@NotNull final Set<String> nodes, @NotNull final String localNode) {
        assert nodes.contains(localNode);

        this.nodes = new ArrayList<>(nodes);
        this.nodes.sort(String::compareTo);

        this.localNode = localNode;
    }

    @NotNull
    @Override
    public String getNodeForKey(@NotNull final ByteBuffer key) {
        int[] nodesHashes = new int[this.nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            nodesHashes[i] = goodFastHash(32).newHasher()
                    .putString(nodes.get(i), StandardCharsets.UTF_8)
                    .putInt(key.hashCode()).hash().hashCode();
        }

        final int[] nodesSort = Arrays.copyOf(nodesHashes, nodesHashes.length);
        Arrays.sort(nodesSort);

        String neededNode = null;
        int i = 0;
        for (final int node : nodesHashes) {
            if (node == nodesSort[0]) {
                neededNode = nodes.get(i);

                break;
            } else {
                i++;
            }
        }
        return neededNode;
    }

    @NotNull
    @Override
    public List<String> getNodesForKey(@NotNull final ByteBuffer id, final int numOfReplicas) {
        int[] nodesHashes = new int[this.nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            nodesHashes[i] = goodFastHash(32).newHasher()
                    .putString(this.nodes.get(i), StandardCharsets.UTF_8)
                    .putInt(id.hashCode()).hash().hashCode();

        }

        final int[] nodesSort = Arrays.copyOf(nodesHashes, nodesHashes.length);
        Arrays.sort(nodesSort);

        String[] repNodes = new String[numOfReplicas];

        for (int j = 0; j < numOfReplicas; j++) {
            int i = 0;
            for (final int node : nodesHashes) {
                if (node == nodesSort[j]) {
                    repNodes[j] = nodes.get(i);
                    break;
                } else {
                    i++;
                }
            }
        }
        return Arrays.asList(repNodes);
    }

    @NotNull
    @Override
    public List<String> getAllNodes() {
        return new ArrayList<>(this.nodes);
    }

    @Override
    public boolean isLocal(final String nodeId) {
        return nodeId.equals(localNode);
    }

    @Override
    public int getSize() {
        return this.nodes.size();
    }
}
