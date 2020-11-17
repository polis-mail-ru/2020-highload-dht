package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

public class ModHashingImpl implements Topology<String> {
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for module topology implementation.
     *
     * @param allNodes - sets with all nodes.
     * @param currentNode - this node.
     */
    public ModHashingImpl(@NotNull final String currentNode, @NotNull final Collection<String> allNodes) {
        assert allNodes.contains(currentNode);
        this.currentNode = currentNode;
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
    }

    @NotNull
    @Override
    public String identifyByKey(@NotNull final byte[] key) {
        return allNodes[(Arrays.hashCode(key) & Integer.MAX_VALUE) % allNodes.length];
    }

    @NotNull
    @Override
    public String[] replicasFor(@NotNull final ByteBuffer key, final int replicas) {
        final String[] rep = new String[replicas];
        int nodeStarterIndex = (key.hashCode() & Integer.MAX_VALUE) % allNodes.length;
        for (int i = 0; i < replicas; i++) {
            rep[i] = allNodes[nodeStarterIndex];
            nodeStarterIndex = (nodeStarterIndex + 1) % allNodes.length;
        }
        return rep;
    }

    @Override
    public int nodeCount() {
        return allNodes.length;
    }

    @NotNull
    @Override
    public String[] allNodes() {
        return allNodes.clone();
    }

    @NotNull
    @Override
    public String getCurrentNode() {
        return currentNode;
    }

    @Override
    public boolean isMe(final String node) {
        return node.equals(currentNode);
    }
}
