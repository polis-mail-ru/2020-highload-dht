package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.hash.Hashing.murmur3_32;

public class RendezvousHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(RendezvousHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;
    final List<Integer> nodeHashes;

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
        this.nodeHashes = new ArrayList<>();
        for (final String node: this.allNodes) {
            nodeHashes.add(murmur3_32().newHasher().putString(node, StandardCharsets.UTF_8).hash().hashCode());
        }
    }

    @NotNull
    @Override
    public String identifyByKey(final byte[] key) {
        final TreeMap<Integer,String> nodesAndHashes = new TreeMap<>();
        for (int i = 0; i < allNodes.length; i++) {
            nodesAndHashes.put(nodeHashes.get(i)
                    + murmur3_32().newHasher().putBytes(key).hash().hashCode(), allNodes[i]);
        }
        final String ownerNode = nodesAndHashes.firstEntry().getValue();
        if (ownerNode == null) {
            log.error("Hash is null");
            throw new IllegalStateException("Hash code can't be equals null");
        }
        return ownerNode;
    }

    @NotNull
    @Override
    public String[] replicasFor(@NotNull ByteBuffer key, int replicas) {
        return new String[0];
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

    @Override
    public String getCurrentNode() {
        return currentNode;
    }
}
