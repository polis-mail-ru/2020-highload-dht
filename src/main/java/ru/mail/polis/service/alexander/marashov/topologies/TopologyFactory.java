package ru.mail.polis.service.alexander.marashov.topologies;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TopologyFactory {

    private TopologyFactory() {
        // Not instantiatable
    }

    /**
     * Construct a {@link Topology} instance.
     *
     * @param nodesSet - set with all the nodes in the cluster.
     * @param local    - local node identifier.
     */
    @NotNull
    public static Topology<String> create(@NotNull final Set<String> nodesSet, @NotNull final String local) {
        return new RendezvousTopology(nodesSet, local);
    }

}
