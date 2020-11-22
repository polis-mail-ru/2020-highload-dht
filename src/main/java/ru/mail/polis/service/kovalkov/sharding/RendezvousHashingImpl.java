package ru.mail.polis.service.kovalkov.sharding;

import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.google.common.hash.Hashing.murmur3_32;

public class RendezvousHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(RendezvousHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for modular topology implementation.
     *
     * @param currentNode - this node.
     * @param allNodes - sets with all nodes.
     */
    public RendezvousHashingImpl(final String currentNode, final Set<String> allNodes) {
        assert allNodes.contains(currentNode);
        this.currentNode = currentNode;
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
    }

    @Override
    @NotNull
    public String identifyByKey(@NotNull final byte[] key) {
        int currentHash;
        int min = Integer.MAX_VALUE;
        String owner = null;
        for (int i = 0; i < allNodes.length; i++) {
            currentHash = murmur3_32().newHasher()
                    .putString(allNodes[i],StandardCharsets.UTF_8).putBytes(key).hash().hashCode();
            if (currentHash < min) {
                min = currentHash;
                owner = allNodes[i];
            }
        }
        if (owner == null) {
            log.error("Hash is null");
            throw new IllegalStateException("Hash code can't be equals null");
        }
        return owner;
    }

    @NotNull
    @Override
    public String[] replicasFor(@NotNull final ByteBuffer key, final int replicas) {
        final String[] rep = new String[replicas];
        final Map<Integer, String> owners = new TreeMap<>();
        final byte[] bytesKey = BufferConverter.unfoldToBytes(key);
        for (int i = 0; i < allNodes.length; i++) {
            owners.put(murmur3_32().newHasher().putString(allNodes[i],StandardCharsets.UTF_8)
                    .putBytes(bytesKey).hash().hashCode(), allNodes[i]);
        }
        int replicasCounter = 0;
        for (final Map.Entry<Integer, String> entry : owners.entrySet() ){
            if (replicas == replicasCounter) {
                break;
            }
            rep[replicasCounter] = entry.getValue();
            replicasCounter++;
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

    @Override
    @NotNull
    public String getCurrentNode() {
        return currentNode;
    }
}
