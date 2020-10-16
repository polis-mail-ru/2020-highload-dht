package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class NewModularTopology implements Topology<String> {

    @NotNull
    private final String[] nodeSet;
    @NotNull
    private final String myNode;

    public NewModularTopology(
            @NotNull final Set<String> nodeSet,
            @NotNull final String myNode) {
        assert nodeSet.contains(myNode);
        this.myNode = myNode;

        this.nodeSet = new String[nodeSet.size()];
        nodeSet.toArray(this.nodeSet);
        Arrays.sort(this.nodeSet);
    }

    @NotNull
    @Override
    public String primaryFor(
            @NotNull final ByteBuffer key) {
        return nodeSet[(key.hashCode() & Integer.MAX_VALUE) % nodeSet.length];
    }

    @Override
    public int sizeOfAllNodesInCluster() {
        return nodeSet.length;
    }

    @Override
    public boolean isMyNode(
            @NotNull final String node) {
        return node.equals(myNode);
    }

    @NotNull
    @Override
    public String[] all() {
        return nodeSet.clone();
    }
}
