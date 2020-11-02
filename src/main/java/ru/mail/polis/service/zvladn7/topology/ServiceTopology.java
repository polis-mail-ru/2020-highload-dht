package ru.mail.polis.service.zvladn7.topology;

import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ServiceTopology implements Topology<String> {

    private static final Logger log = LoggerFactory.getLogger(ServiceTopology.class);
    public static final int VIRTUAL_NODES_PER_NODE = 10;

    @NotNull
    private final String local;
    @NotNull
    private final SortedMap<Integer, String> hashRing;

    /**
     * Service topology to represent the node of cluster.
     * Also help to decide where the date should be stored.
     * @param nodeSet - set of cluster nodes
     * @param local   - node which represented by this topology
     */
    public ServiceTopology(@NotNull final Set<String> nodeSet, @NotNull final String local) {
        assert nodeSet.contains(local);
        this.local = local;
        this.hashRing = new TreeMap<>();

        final String[] nodes = nodeSet.toArray(new String[0]);
        Arrays.sort(nodes);
        for (final String node : nodes) {
            attachToRing(node);
        }
    }

    @NotNull
    @Override
    public String nodeFor(@NotNull final ByteBuffer key) {
        int hash = key.hashCode();
        if (!hashRing.containsKey(hash)) {
            final SortedMap<Integer, String> tailMap = hashRing.tailMap(hash);
            hash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
        }
        return hashRing.get(hash);
    }

    @NotNull
    @Override
    public Set<String> nodesForKey(@NotNull final ByteBuffer key, final int from) {
        final int hash = key.hashCode();
        final SortedMap<Integer, String> tailMap = hashRing.tailMap(hash);
        final Set<String> nodesForReplication = new HashSet<>();
        addReplicasForKey(nodesForReplication, tailMap, from);
        if (nodesForReplication.size() < from) {
            addReplicasForKey(nodesForReplication, hashRing, from);
        }

        return nodesForReplication;
    }

    private void addReplicasForKey(final Set<String> nodesForReplication,
                                   final SortedMap<Integer, String> map,
                                   final int from) {
        for (final Map.Entry<Integer, String> e : map.entrySet()) {
            if (nodesForReplication.size() == from) {
                break;
            }
            nodesForReplication.add(e.getValue());
        }
    }

    @Override
    public boolean isLocal(@NotNull final String node) {
        return node.equals(local);
    }

    @Override
    public boolean isLocal(@NotNull final Set<String> nodes) {
        return nodes.contains(local);
    }

    @Override
    public int size() {
        return hashRing.size();
    }

    @Override
    public String[] nodes() {
        return hashRing.values()
                .stream()
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public String local() {
        return local;
    }

    private void attachToRing(@NotNull final String node) {
        log.debug("Start attaching node: {} to hash ring", node);
        for (int i = 0; i < VIRTUAL_NODES_PER_NODE; ++i) {
            String stringToHash = node + i;
            int hashCode = Hash.murmur3(stringToHash);
            while (hashRing.containsKey(hashCode)) {
                stringToHash = node + i;
                hashCode = Hash.murmur3(stringToHash);
            }
            log.debug("{} and {} : {}", node, i, hashCode);
            hashRing.put(hashCode, node);
        }
    }
}
