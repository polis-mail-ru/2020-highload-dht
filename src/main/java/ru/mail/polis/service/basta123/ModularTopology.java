package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModularTopology implements Topology<String> {

    private final List<String> nodes;
    @NotNull
    private final String localNode;

    /**
     * Initialization and preparation of nodes.
     *
     * @param nodes     - cluster nodes.
     * @param localNode - our node.
     */
    public ModularTopology(@NotNull final Set<String> nodes, @NotNull final String localNode) {
        assert nodes.contains(localNode);

        this.nodes = new ArrayList<>(nodes);
        this.nodes.sort(String::compareTo);

        this.localNode = localNode;
    }

    @NotNull
    @Override
    public String getNodeForKey(@NotNull final ByteBuffer id) {
        System.out.println(id);
        return this.nodes.get((id.hashCode() & Integer.MAX_VALUE) % this.nodes.size());
    }

    @NotNull
    @Override
    public List<String> getNodesForKey(@NotNull final ByteBuffer id, final int numOfReplicas) {
        int nodeIndex = (id.hashCode() & Integer.MAX_VALUE) % this.nodes.size();
        final ArrayList<String> nodesFrom = new ArrayList<>();
        for (int i = 0; i < numOfReplicas; i++) {
            nodesFrom.add(this.nodes.get(nodeIndex));
            nodeIndex = (nodeIndex + 1) % this.nodes.size();
        }

        return nodesFrom;
    }

    @Override
    public boolean isLocal(@NotNull final String nodeId) {
        return nodeId.equals(localNode);
    }

    @Override
    public int getSize() {
        return this.nodes.size();
    }

    @NotNull
    @Override
    public List<String> getAllNodes() {
        return new ArrayList<>(this.nodes);
    }

}
