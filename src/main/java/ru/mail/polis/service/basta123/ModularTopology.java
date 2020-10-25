package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Topology;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModularTopology implements Topology<String> {

    List<String> nodes;
    @NotNull
    private final String localNode;

    /**
     * Initialization and preparation of nodes.
     *
     * @param nodes     - cluster nodes.
     * @param localNode - our node.
     */
    public ModularTopology(@NotNull final Set<String> nodes,
                           @NotNull final String localNode) {

        assert nodes.contains(localNode);

        this.nodes = new ArrayList<>(nodes);
        this.nodes.sort(String::compareTo);

        this.localNode = localNode;
    }

    @Override
    public int size() {
        return this.nodes.size();
    }

    @NotNull
    @Override
    public String getNode(@NotNull final ByteBuffer key) {
        return this.nodes.get((key.hashCode() & Integer.MAX_VALUE) % this.nodes.size());

    }

    @NotNull
    @Override
    public boolean isLocal(@NotNull final String node) {
        return node.equals(this.localNode);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getAllNodes() {
        final ArrayList<String> arrayNodes = (ArrayList<String>)this.nodes;
        final Object object = arrayNodes.clone();
        return (ArrayList<String>)object;
    }
}
