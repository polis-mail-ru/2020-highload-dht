package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

public class ModularPolicy implements ShardingPolicy<ByteBuffer, String> {
    @NotNull
    private final String[] nodeUrls;
    @NotNull
    private final String homeNode;
    @NotNull
    private final Function<ByteBuffer, Integer> keysHashFunction;
    
    /**
     * ModularPolicy's constructor.
     *
     * @param nodes        list of nodes' urls
     * @param hashFunction hash function for key
     * @param homeNode     local node's url
     */
    public ModularPolicy(@NotNull final Set<String> nodes,
                         @NotNull final Function<ByteBuffer, Integer> hashFunction,
                         @NotNull final String homeNode) {
        assert !nodes.isEmpty();
        assert nodes.contains(homeNode);
        
        this.nodeUrls = new String[nodes.size()];
        this.homeNode = homeNode;
        nodes.toArray(this.nodeUrls);
        Arrays.sort(this.nodeUrls);
        this.keysHashFunction = hashFunction;
    }
    
    /**
     * Implements simple sharding policy by modular dividing hash function.
     *
     * @param key records's key
     * @return node's url
     */
    @NotNull
    @Override
    public String getNode(@NotNull final ByteBuffer key) {
        return nodeUrls[(keysHashFunction.apply(key) & Integer.MAX_VALUE) % nodeUrls.length];
    }
    
    @NotNull
    @Override
    public String[] getNodeReplicas(@NotNull final ByteBuffer key,
                                    final int replicas) {
        final long startNodeId = keysHashFunction.apply(key) & Integer.MAX_VALUE;
        final String[] nodeReplicas = new String[replicas];
        for (int i = 0; i < replicas; i++) {
            nodeReplicas[i] = nodeUrls[(int) ((startNodeId + i) % nodeUrls.length)];
        }
        return nodeReplicas;
    }
    
    @NotNull
    @Override
    public String[] all() {
        return nodeUrls.clone();
    }
    
    @NotNull
    @Override
    public String homeNode() {
        return homeNode;
    }
}
