package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;
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
        final byte[] keyBytes = new byte[key.remaining()];
        key.duplicate().get(keyBytes).clear();
        final Map<Integer,String> nodesAndHashes = new TreeMap<>();
        final int[] nodeIdentifier = new int[allNodes.length];
        final int[] hashes = new int[allNodes.length];
        for (int i = 0; i < allNodes.length; i++) {
            nodeIdentifier[i] = allNodes[i].chars().sum();
            hashes[i] = murmur3_32().newHasher().putInt(nodeIdentifier[i]).putBytes(keyBytes).hash().hashCode();
            nodesAndHashes.put(hashes[i],allNodes[i]);
        }
        final String ownerNode = nodesAndHashes.get(nodesAndHashes.keySet()
                .stream().findFirst().orElse(null));
        if (ownerNode == null) {
            log.error("Hash is null");
            throw new RuntimeException("Hash code can't be equals null");
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
}
