package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
        List<String> nodes = getNodesForKey(key, 1);
        return nodes.get(0);
    }

    @NotNull
    @Override
    public List<String> getNodesForKey(@NotNull final ByteBuffer id, final int numOfReplicas) {
        byte[] key = Utils.getByteArrayFromByteBuffer(id);

        Queue<NodeHash> queue = new PriorityQueue<>();
        for (int i = 0; i < nodes.size(); i++) {
            final String node = this.nodes.get(i);
            queue.add(new NodeHash(node,
                    goodFastHash(32).newHasher()
                            .putString(node, StandardCharsets.UTF_8)
                            .putBytes(key).hash().hashCode()
            ));
        }
        List<String> repNodes=new ArrayList<>();
        int i=0;
        while (!queue.isEmpty() && i<numOfReplicas) {
            i++;
            repNodes.add(queue.poll().getNode());
        }

        return repNodes;
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
