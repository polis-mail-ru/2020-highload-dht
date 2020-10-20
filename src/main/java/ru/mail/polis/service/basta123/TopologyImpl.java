package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Topology;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class TopologyImpl implements Topology<String> {

    @NotNull
    private final String[] nodes;
    @NotNull
    private final String localNode;

    /**
     * Initialization and preparation of nodes.
     *
     * @param nodes - cluster nodes.
     * @param localNode - our node.
     */
    
   public TopologyImpl(@NotNull final Set<String> nodes,
                       @NotNull final String localNode) {

      assert nodes.contains(localNode);

       this.nodes = new String[nodes.size()];
       nodes.toArray(this.nodes);
       Arrays.sort(this.nodes);

       this.localNode = localNode;

   }

    @Override
    public int size() {
        return nodes.length;
    }

    @NotNull
    @Override
    public String getNode(@NotNull final ByteBuffer key) {
        return this.nodes[(key.hashCode() & Integer.MAX_VALUE) % this.nodes.length];
    }

    @NotNull
    @Override
    public boolean isLocal(@NotNull final String node) {
        return node.equals(this.localNode);
    }

    @NotNull
    @Override
    public String[] getAllNodes() {
        return nodes.clone();
    }
}
