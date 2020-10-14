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
