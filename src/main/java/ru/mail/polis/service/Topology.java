package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Topology {

    private final List<String> clusterNodes;
    private final String id;

    Topology(@NotNull final Set<String> nodes, @NotNull final String id) {
        this.clusterNodes = new ArrayList<>(nodes);
        this.id = id;
    }

    Set<String> getNodes() {
        return new HashSet<>(this.clusterNodes);
    }

<<<<<<< HEAD
=======
    String getCurrentNode() {
        return id;
    }

    int getSize() {
        return this.clusterNodes.size();
    }

>>>>>>> task_5
    boolean isSelfId(@NotNull final String nodeId) {
        return nodeId.equals(id);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return clusterNodes.get((key.hashCode() & Integer.MAX_VALUE) % clusterNodes.size());
    }
<<<<<<< HEAD
=======

    String[] getReplicas(@NotNull final ByteBuffer id, final int numOfReplicas) {

        int nodeIndex = (id.hashCode() & Integer.MAX_VALUE) % clusterNodes.size();
        final String[] nodeReplicas = new String[numOfReplicas];

        for (int i = 0; i < numOfReplicas; i++) {
            nodeReplicas[i] = clusterNodes.get(nodeIndex);
            nodeIndex = (nodeIndex + 1) % clusterNodes.size();
        }

        return nodeReplicas;
    }
>>>>>>> task_5
}
