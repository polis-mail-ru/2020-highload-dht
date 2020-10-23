package ru.mail.polis.service.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Modular topology.
 *
 * @author kate
 */
public class ModuleTopology implements Topology<String> {
    private final List<String> nodes;
    private final String me;

    /**
     * Modular topology constructor.
     *
     * @param nodes - cluster.
     * @param me - name of the current node.
     */
    public ModuleTopology(
            @NotNull final Set<String> nodes,
            @NotNull final String me) {
        assert nodes.size() > 0;
        this.me = me;
        assert nodes.contains(me);
        this.nodes = new ArrayList<>(nodes);
        Collections.sort(this.nodes);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        return nodes.get((key.hashCode() & Integer.MAX_VALUE) % nodes.size());
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public List<String> all() {
        return nodes;
    }
}
