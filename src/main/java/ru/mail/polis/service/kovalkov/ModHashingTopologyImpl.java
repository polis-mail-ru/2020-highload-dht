package ru.mail.polis.service.kovalkov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.hash.Hashing.murmur3_32;

public class ModHashingTopologyImpl implements Topology <String> {
    private static final Logger log = LoggerFactory.getLogger(ModHashingTopologyImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for topology implementation.
     *
     * @param allNodes - sets with all nodes.
     * @param currentNode - this node.
     */
    public ModHashingTopologyImpl(final String currentNode, final Set<String> allNodes) {
        if(!allNodes.contains(currentNode)) {
            log.error("This node - {} is not a part of cluster {}", currentNode, allNodes);
            throw new RuntimeException("Current not is invalid.");
        }
        this.currentNode = currentNode;
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
    }

    @Override
    public String identifyByKey(final ByteBuffer key) {
        return allNodes[(murmur3_32().newHasher().putBytes(key.duplicate()).hash().hashCode()
                & Integer.MAX_VALUE) % nodeCount()];
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
