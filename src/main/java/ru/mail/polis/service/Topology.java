package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
        this.clusterNodes.sort(String::compareTo);
        this.id = id;
    }

    Set<String> getNodes() {
        return new HashSet<>(clusterNodes);
    }

    String getCurrentNode() {
        return id;
    }

    int getSize() {
        return clusterNodes.size();
    }

    boolean isSelfId(@NotNull final String nodeId) {
        return nodeId.equals(id);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return clusterNodes.get((key.hashCode() & Integer.MAX_VALUE) % clusterNodes.size());
    }

    Set<String> getReplicas(@NotNull final ByteBuffer id, final int numOfReplicas) throws IOException {

//        if (numOfReplicas > clusterNodes.size()) {
//            throw new IOException(
//                    String.format(
//                            "Not enough nodes in cluster. Requested %d, though only %d exists in cluster",
//                            numOfReplicas, clusterNodes.size()
//                    )
//            );
//        }

        int nodeIndex = (id.hashCode() & Integer.MAX_VALUE) % clusterNodes.size();
        final Set<String> nodeReplicas = new HashSet<>();

        for (int i = 0; i < numOfReplicas; i++) {
            nodeReplicas.add(clusterNodes.get(nodeIndex));
            nodeIndex = (nodeIndex + 1) % clusterNodes.size();
        }

        return nodeReplicas;
    }
}
