package ru.mail.polis.service.codearound;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModularTopology implements Topology<String> {

    @NotNull
    private final String[] nodeArray;
    @NotNull
    private final String thisNode;

    /**
     * topology impl class const.
     *
     * @param nodes - set of cluster-establishing nodes
     * @param thisNode - node which is distinguished as a current request handler
     */
    public ModularTopology(@NotNull final Set<String> nodes, @NotNull final String thisNode) {
        assert nodes.contains(thisNode);
        nodeArray = new String[nodes.size()];
        this.thisNode = thisNode;
        nodes.toArray(this.nodeArray);
        Arrays.sort(this.nodeArray);
    }

    /**
     * retrieves node associate ID for key searched.
     *
     * @param id - node ID
     * @return destination node ID
     */
    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer id) {
        final int node = (id.hashCode() & Integer.MAX_VALUE) % nodeArray.length;
        return nodeArray[node];
    }

    /**
     * generates array to collect replica IDs inside of.
     *
     * @param id - node ID
     * @param numOfReplicas - number of node replicas
     * @return array of String-defined IDs
     */
    @NotNull
    @Override
    public String[] replicasFor(@NotNull final ByteBuffer id, final int numOfReplicas) {
        int nodeIndex = (id.hashCode() & Integer.MAX_VALUE) % nodeArray.length;
        final String[] nodeReplicas = new String[numOfReplicas];
        for (int i = 0; i < numOfReplicas; i++) {
            nodeReplicas[i] = nodeArray[nodeIndex];
            nodeIndex = (nodeIndex + 1) % nodeArray.length;
        }
        return nodeReplicas;
    }

    /**
     * evaluates if current node ID matches target one.
     *
     * @param nodeId - node ID which REST handler enforced to execute request-specific function at the moment
     * @return true if ID match found, otherwise false
     */
    @Override
    public boolean isThisNode(@NotNull final String nodeId) {
        return nodeId.equals(thisNode);
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
    @NotNull
    @Override
    public Set<String> getNodes() {
        return Set.of(nodeArray);
    }

    /**
     * retrieves node that is processing a specific request at a moment.
     *
     * @return String-defined node ID
     */
    @NotNull
    @Override
    public String getThisNode() {
        return thisNode;
    }
}
