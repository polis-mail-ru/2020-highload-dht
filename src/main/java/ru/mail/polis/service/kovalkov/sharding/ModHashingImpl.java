package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(ModHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for modular topology implementation.
     *
     * @param allNodes - sets with all nodes.
     * @param currentNode - this node.
     */
    public ModHashingImpl(final String currentNode, final Set<String> allNodes) {
        if (!allNodes.contains(currentNode)) {
            log.error("This node - {} is not a part of cluster {}", currentNode, allNodes);
            throw new RuntimeException("Current not is invalid.");
        }
        this.currentNode = currentNode;
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
    }

    @NotNull
    @Override
    public String identifyByKey(@NotNull final ByteBuffer key) {
        return allNodes[(key.hashCode() & Integer.MAX_VALUE) % nodeCount()];
    }

    @NotNull
    @Override
    public String[] replicasFor(@NotNull final ByteBuffer key, final int replicas) {
        final String[] rep = new String[replicas];
        int nodeStarterIndex = (key.hashCode() & Integer.MAX_VALUE) % nodeCount();
        for (int i = 0; i < replicas; i++) {
            rep[i] = allNodes[nodeStarterIndex];
            nodeStarterIndex = (nodeStarterIndex + 1) % nodeCount();
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

    @Override
    public boolean isMe(final String node) {
        return node.equals(currentNode);
    }
}
