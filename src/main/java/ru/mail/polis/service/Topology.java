package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Topology {

    private final List<String> clusterNode;
    private final String id;

    Topology(@NotNull final Set<String> nodes, @NotNull final String id) {
        this.clusterNode = new ArrayList<>(nodes);
        this.id = id;
    }

    Set<String> getNodes() {
        return new HashSet<>(this.clusterNode);
    }

    Boolean isSelfId(@NotNull final String nodeId) {
        return nodeId.equals(id);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return clusterNode.get((key.hashCode() & Integer.MAX_VALUE) % clusterNode.size());
    }
}
