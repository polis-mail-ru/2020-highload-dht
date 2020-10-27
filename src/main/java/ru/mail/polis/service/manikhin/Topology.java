package ru.mail.polis.service.manikhin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    Set<String> getReplicas(@NotNull final ByteBuffer key, @NotNull final Replicas replicas) {
        final Set<String> result = new HashSet<>();
        int index = key.hashCode() & Integer.MAX_VALUE % nodes.size();

        while (result.size() < replicas.getFrom()) {
            result.add(nodes.get(index));
            index++;

            if (index == nodes.size()) {
                index = 0;
            }
        }

        return result;
    }
}
