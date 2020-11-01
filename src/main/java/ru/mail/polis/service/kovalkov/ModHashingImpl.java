package ru.mail.polis.service.kovalkov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

public class ModHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(ModHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for module topology implementation.
     *
     * @param allNodes - sets with all nodes.
     * @param currentNode - this node.
     */
    public ModHashingImpl(final String currentNode, final Set<String> allNodes) {
        if (!allNodes.contains(currentNode)) {
            log.error("This node - {} is not a part of cluster {}", currentNode, allNodes);
            throw new IllegalArgumentException("Current not is invalid.");
        }
        this.currentNode = currentNode;
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
    }

    @Override
    public String identifyByKey(final byte[] key) {
        return allNodes[(Arrays.hashCode(key) & Integer.MAX_VALUE) % nodeCount()];
    }

    @Override
    public int nodeCount() {
        return allNodes.length;
    }

    @Override
    public String[] allNodes() {
        return allNodes.clone();
    }

    @Override
    public boolean isMe(final String node) {
        return node.equals(currentNode);
    }
}
