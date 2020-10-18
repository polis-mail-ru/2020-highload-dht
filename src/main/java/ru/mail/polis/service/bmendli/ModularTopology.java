package ru.mail.polis.service.bmendli;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ModularTopology implements Topology<String> {

    @NotNull
    private final String local;
    @NotNull
    private final String[] nodes;

    /**
     * Constructor of {@link ModularTopology}
     *
     * @param nodes cluster
     * @param local current node
     */
    public ModularTopology(
            @NotNull final Set<String> nodes,
            @NotNull final String local) {
        assert nodes.contains(local);

        this.local = local;
        this.nodes = new String[nodes.size()];
        nodes.toArray(this.nodes);
        Arrays.sort(this.nodes);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        final int hash = key.hashCode();
        final int index = (hash & Integer.MAX_VALUE) % nodes.length;
        return nodes[index];
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

    @Override
    public boolean isLocal(@NotNull final String node) {
        return local.equals(node);
    }
}
