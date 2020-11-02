package ru.mail.polis.service.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public ModuleTopology(@NotNull final Set<String> nodes, @NotNull final String me) {
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

    @NotNull
    @Override
    public Set<String> primaryFor(@NotNull final ByteBuffer key, @NotNull final Replicas replicas) {
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
        return new ArrayList<>(nodes);
    }
}
