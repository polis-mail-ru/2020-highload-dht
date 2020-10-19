package ru.mail.polis.service.valaubr.topology;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModularTopology implements Topology<String> {
    @NotNull
    private final String[] nodes;
    @NotNull
    private final String me;

    /**
     * topology of servise ant methods to work for him.
     *
     * @param nodes - node list
     * @param me - current server
     */
    public ModularTopology(@NotNull final Set<String> nodes, @NotNull final String me) {
        assert nodes.contains(me);
        this.nodes = new String[nodes.size()];
        this.me = me;
        nodes.toArray(this.nodes);
        Arrays.sort(this.nodes);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        return nodes[(Math.abs(key.hashCode()) & Integer.MAX_VALUE) % nodes.length];
    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public String[] all() {
        return nodes.clone();
    }

    @Override
    public int size() {
        return nodes.length;
    }
}
