package ru.mail.polis.service.manikhin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Topology {

    private final List<String> nodes;
    private final String currentNodeId;

    /**
     * Topology initialization.
     *
     * @param nodes - nodes list
     * @param currentNodeId - current node identified
     */
    public Topology(@NotNull final Set<String> nodes, @NotNull final String currentNodeId) {
        this.nodes = new ArrayList<>(nodes);
        this.nodes.sort(String::compareTo);
        this.currentNodeId = currentNodeId;
    }

    String getId() {
        return this.currentNodeId;
    }

    Set<String> getNodes() {
        return new HashSet<>(this.nodes);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return nodes.get((key.hashCode() & Integer.MAX_VALUE) % nodes.size());
    }
}
