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

   public TopologyImpl(@NotNull Set<String> nodes,
                       @NotNull String localNode) {

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
        return nodes[(key.hashCode() & Integer.MAX_VALUE) % nodes.length];
    }

    @NotNull
    @Override
    public boolean isLocal(@NotNull final String node) {
        return node.equals(localNode);
    }

    @NotNull
    @Override
    public String[] getAllNodes() {
        return nodes.clone();
    }
}
