package ru.mail.polis.service.ivanovandrey;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;

public class SimpleTopology {

    private final String[] nodes;
    private final String me;

    /**
     * Constructor.
     *
     * @param topology - topology.
     * @param me - current node.
     */
    public SimpleTopology(@NotNull final Set<String> topology,
                          @NotNull final String me) {
        this.nodes = new String[topology.size()];
        topology.toArray(this.nodes);
        this.me = me;
    }

    String getMe() {
        return this.me;
    }

    String[] getNodes() {
        return nodes.clone();
    }

    String primaryFor(@NotNull final ByteBuffer key) {
        return nodes[(key.hashCode() & Integer.MAX_VALUE) % nodes.length];
    }
}
