package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModuleTopology implements Topology<String> {
    private final String[] nodes;
    private final String me;

    public ModuleTopology(
            @NotNull final Set<String> nodes,
            @NotNull final String me) {

        this.me = me;
        assert nodes.contains(me);
        this.nodes = new String[nodes.size()];
        nodes.toArray(this.nodes);
        Arrays.sort(this.nodes);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull ByteBuffer key) {
        return nodes[(key.hashCode() & Integer.MAX_VALUE) % nodes.length];
    }

    @Override
    public int size() {
        return nodes.length;
    }

    @Override
    public boolean isMe(@NotNull String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public String[] all() {
        return nodes.clone();
    }
}
