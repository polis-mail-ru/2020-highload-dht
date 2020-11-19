package ru.mail.polis.service.ivanovandrey;

import one.nio.util.Hash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RandezvouzTopology {

    private final String thisNode;

    /**
     * Constructor.
     */
    public RandezvouzTopology(final int port) {
        this.thisNode = "http://localhost:" + port;
    }

    public Boolean isCurrentNode(final String node) {
        return node.equals(this.thisNode);
    }

    /**
     * Returns nodes that stores data for a given key by rendezvous hashing algorithm.
     *
     * @param nodes - list of existing nodes
     * @param key - data id
     * @param replicasNumber - number of nodes to store data
     */
    public Set<String> getNodes(final Set<String> nodes,
                                       final String key,
                                       final int replicasNumber) {
        final Map<Integer,String> hash = new HashMap<>();
        final Set<String> resultNodes = new HashSet<>();
        for (final String node : nodes) {
            hash.put(Hash.murmur3(node + key), node);
        }
        final Object[] keys = hash.keySet().toArray();
        Arrays.sort(keys);
        for (int i = keys.length - replicasNumber; i < keys.length; i++) {
            resultNodes.add(hash.get(keys[i]));
        }
        return resultNodes;
    }
}
