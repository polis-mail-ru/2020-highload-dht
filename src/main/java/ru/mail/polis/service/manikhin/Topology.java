package ru.mail.polis.service.manikhin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;

public class Topology {

    private final List<String> nodes;
    private final String id;

    /**
     * Topology initialization.
     *
     * @param nodes - nodes list
     * @param id - current node identified
     */
    public Topology(@NotNull final Set<String> nodes, @NotNull final String id) {
        this.nodes = new ArrayList<>(nodes);
        this.id = id;
        Collections.sort(this.nodes);
    }

    String getId() {
        return this.id;
    }

    Set<String> getNodes() {
        return new HashSet<>(this.nodes);
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return nodes.get((key.hashCode() & Integer.MAX_VALUE) % nodes.size());
    }
}
