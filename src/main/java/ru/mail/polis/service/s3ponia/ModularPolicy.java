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

    public ModularPolicy(@NotNull Set<String> nodes, @NotNull Function<ByteBuffer, Integer> hashFunction,
                         @NotNull final String homeNode) {
        assert nodes.size() > 0;
        assert nodes.contains(homeNode);

        this.nodeUrls = new String[nodes.size()];
        this.homeNode = homeNode;
        nodes.toArray(this.nodeUrls);
        Arrays.sort(this.nodeUrls);
        this.keysHashFunction = hashFunction;
    }

    /**
     * Implements simple sharding policy by modular dividing hash function.
     * @param key records's key
     * @return node's url
     */
    @NotNull
    @Override
    public String getNode(@NotNull ByteBuffer key) {
        return nodeUrls[(keysHashFunction.apply(key) & Integer.MAX_VALUE) % nodeUrls.length];
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
