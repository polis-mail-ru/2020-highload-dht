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
    private static final Logger log = LoggerFactory.getLogger(RendezvousHashingImpl.class);
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
            log.error("This node - {} is not a part of cluster", currentNode);
            throw new IllegalArgumentException("Current not is invalid.");
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
        final Map<Integer,String> nodesAndHashes = new TreeMap<>();
        for (int i = 0; i < allNodes.length; i++) {
            nodesAndHashes.put(
                    murmur3_32().newHasher().putInt(allNodes[i]
                            .chars().sum()).putBytes(keyBytes).hash().hashCode(),allNodes[i]);
        }
        final String ownerNode = nodesAndHashes.get(nodesAndHashes.keySet()
                .stream().findFirst().orElse(null));
        if (ownerNode == null) {
            log.error("Hash is null");
            throw new IllegalStateException("Hash code can't be equals null");
        }
        return ownerNode;
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
