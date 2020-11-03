package ru.mail.polis.service.stakenschneider;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Nodes {

    private final List<String> clusterNode;
    private final String id;

    public Nodes(@NotNull final Set<String> nodes, @NotNull final String id) {
        this.clusterNode = new ArrayList<>(nodes);
        this.id = id;
    }

    /**
     * Get the clusters ids.
     *
     * @param count - the amount of replicas
     * @param key - key id
     * @return array ids of the clusters to create replicas
     */
    public String[] replicas(final int count, @NotNull final ByteBuffer key) {
        final String[] res = new String[count];
        int index = (key.hashCode() & Integer.MAX_VALUE) % clusterNode.size();
        for (int j = 0; j < count; j++) {
            res[j] = clusterNode.get(index);
            index = (index + 1) % clusterNode.size();
        }
        return res;
    }

    String getId() {
        return this.id;
    }

    Set<String> getNodes() {
        return new HashSet<>(this.clusterNode);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return clusterNode.get((key.hashCode() & Integer.MAX_VALUE) % clusterNode.size());
    }
}
