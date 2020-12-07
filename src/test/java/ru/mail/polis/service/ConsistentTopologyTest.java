package ru.mail.polis.service;

import org.junit.jupiter.api.Test;
import ru.mail.polis.service.mrsandman5.clustering.ConsistentHashingTopology;
import ru.mail.polis.service.mrsandman5.clustering.Topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentTopologyTest extends ClusterTestBase{

    private static final int VNODE_COUNT = 100;
    private static final int KEYS_COUNT = 1_000;

    @Override
    int getClusterSize() {
        return 8;
    }

    @Test
    void basicReplication() {
        for (final String node : nodes) {
            final Topology<String> first = new ConsistentHashingTopology<>(Set.of(nodes), node, VNODE_COUNT);
            final Topology<String> second = new ConsistentHashingTopology<>(Set.of(nodes), node, VNODE_COUNT);
            for (int i = 0; i < KEYS_COUNT; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodesFirst = first.primaryFor(key);
                final String nodesSecond = second.primaryFor(key);
                assertEquals(nodesFirst, nodesSecond);
            }
        }
    }
    @Test
    void multipleNodes() {
        for (final String node : nodes) {
            final Topology<String> topology = new ConsistentHashingTopology<>(Set.of(nodes), node, VNODE_COUNT);
            final Map<String, Integer> distr = new HashMap<>();
            for (int i = 0; i < KEYS_COUNT; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodeForKey = topology.primaryFor(key);
                if (!distr.containsKey(nodeForKey)) {
                    distr.put(nodeForKey, 0);
                }
                distr.put(nodeForKey, distr.get(nodeForKey) + 1);
            }
            distr.values().forEach(i -> assertTrue(i != 0));
        }
    }

}
