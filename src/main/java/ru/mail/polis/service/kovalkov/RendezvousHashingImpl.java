package ru.mail.polis.service.kovalkov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.hash.Hashing.murmur3_32;

public class RendezvousHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(ModHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for modular topology implementation.
     *
     * @param allNodes - sets with all nodes.
     * @param currentNode - this node.
     */
    public RendezvousHashingImpl(final String currentNode, final Set<String> allNodes) {
        if (!allNodes.contains(currentNode)) {
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
        final byte[] keyBytes = new byte[key.remaining()];
        key.duplicate().get(keyBytes).clear();
        Map<Integer,String> nodesAndHashes = new TreeMap<>();
        final int[] nodeIdentifier = new int[allNodes.length];
        final int[] hashes = new int[allNodes.length];
        for (int i = 0; i < allNodes.length; i++) {
            nodeIdentifier[i] = allNodes[i].chars().sum();
            hashes[i] = murmur3_32().newHasher().putInt(nodeIdentifier[i]).putBytes(keyBytes).hash().hashCode();
            nodesAndHashes.put(hashes[i],allNodes[i]);
        }
        return nodesAndHashes.get(nodesAndHashes.keySet().stream().findFirst().get());
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
