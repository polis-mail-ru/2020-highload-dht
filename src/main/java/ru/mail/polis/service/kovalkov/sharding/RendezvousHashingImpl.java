package ru.mail.polis.service.kovalkov.sharding;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.hash.Hashing.murmur3_32;

public class RendezvousHashingImpl implements Topology<String> {
    private static final Logger log = LoggerFactory.getLogger(RendezvousHashingImpl.class);
    private final String[] allNodes;
    private final String currentNode;

    /**
     * Constructor for modular topology implementation.
     *
     * @param currentNode - this node.
     * @param allNodes    - sets with all nodes.
     */
    public RendezvousHashingImpl(final String currentNode, final Set<String> allNodes) {
        assert allNodes.contains(currentNode);
        this.allNodes = new String[allNodes.size()];
        allNodes.toArray(this.allNodes);
        Arrays.sort(this.allNodes);
        this.currentNode = currentNode;
    }

    @Override
    @NotNull
    public String identifyByKey(@NotNull final byte[] key) {
        int currentHash;
        int min = Integer.MAX_VALUE;
        String owner = null;
        for (final String node : allNodes) {
            currentHash = murmur3_32().newHasher()
                    .putString(node, StandardCharsets.UTF_8).putBytes(key).hash().hashCode();
            if (currentHash < min) {
                min = currentHash;
                owner = node;
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
        final int[] owners = IntStream.generate(() -> Integer.MAX_VALUE).limit(replicas).toArray();
        int firstMax = 0;
        int currentHash;
        for (int i = 0; i < allNodes.length; i++) {
            currentHash = murmur3_32().newHasher()
                    .putString(allNodes[i], StandardCharsets.UTF_8).putInt(key.hashCode()).hash().hashCode();
            if (currentHash <= owners[firstMax]) {
                owners[firstMax] = currentHash;
                rep[firstMax] = allNodes[i];
                firstMax = fistMax(owners);
            }
        }
        return rep;
    }

    private static int fistMax(@NotNull final int[] owners) {
        int maxHash = owners[0];
        int index = 0;
        for (int i = 0; i < owners.length; i++) {
            if (owners[i] > maxHash) {
                maxHash = owners[i];
                index = i;
            }
        }
        return index;
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
