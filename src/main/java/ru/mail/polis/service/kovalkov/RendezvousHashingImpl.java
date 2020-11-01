package ru.mail.polis.service.kovalkov;

import com.google.common.hash.Hasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.hash.Hashing.murmur3_32;

public class RendezvousHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(RendezvousHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;
    final Map<String, Hasher> nodeHashes;

    /**
     * Constructor for modular topology implementation.
     *
     * @param currentNode - this node.
     * @param allNodes - sets with all nodes.
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
        this.nodeHashes = new HashMap<>();
        for (final String node: this.allNodes) {
            nodeHashes.put(node, murmur3_32().newHasher().putString(node, StandardCharsets.UTF_8));
        }
    }

    @Override
    public String identifyByKey(final byte[] key) {
        final TreeMap<Integer,String> nodesAndHashes = new TreeMap<>();
        for ( final Map.Entry<String, Hasher> entry : nodeHashes.entrySet()) {
            nodesAndHashes.put(entry.getValue().putBytes(key).hash().hashCode(), entry.getKey());
        }
        final String ownerNode = nodesAndHashes.firstEntry().getValue();
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
