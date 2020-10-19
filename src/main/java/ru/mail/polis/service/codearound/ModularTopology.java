package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModularTopology implements Topology<String> {

    @NotNull
    private final String[] nodeArray;
    @NotNull
    private final String self;

    /**
     * topology impl class const.
     *
     * @param nodes set of cluster-building nodes
     * @param self node which is distinguished as a current request handler
     */
    public ModularTopology(@NotNull final Set<String> nodes, @NotNull final String self) {

        assert nodes.contains(self);

        nodeArray = new String[nodes.size()];
        this.self = self;

        nodes.toArray(nodeArray);
        Arrays.sort(nodeArray);
    }

    /**
     * retrieves node associate ID for key searched.
     *
     * @param key - key searched
     * @return target node ID
     */
    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        return nodeArray[(key.hashCode() & Integer.MAX_VALUE) % nodeArray.length];
    }

    /**
     * evaluates if current node ID matches target one.
     *
     * @param nodeId currently working node ID
     * @return true if ID match found, otherwise false
     */
    @Override
    public boolean isSelfId(@NotNull final String nodeId) {
        return nodeId.equals(self);
    }

    /**
     * retrieves number of nodes that make up the cluster.
     *
     * @return number of nodes
     */
    @Override
    public int getClusterSize() {
        return nodeArray.length;
    }

    /**
     * retrieves array of node IDs belonging the cluster.
     *
     * @return array of nodes
     */
    @Override
    public String[] getNodes() {
        return nodeArray.clone();
    }
}
