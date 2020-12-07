package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.mrsandman5.clustering.ConsistentHashingTopology;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentTopologyTest extends ClusterTestBase {

    private static final int VNODES = 100;
    private static final int KEYS = 100_000;
    private static final float DELTA = 0.1f;
    private static final ReplicasFactor RF = ReplicasFactor.create(3, 4);
    private static final Set<String> NODES = Set.of("http://localhost:8080",
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083");

    @Override
    int getClusterSize() {
        return NODES.size();
    }

    @Test
    void consistency() {
        for (final String node : nodes) {
            final Topology<String> first = createTopology(node);
            final Topology<String> second = createTopology(node);
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodeFirst = first.primaryFor(key);
                final String nodeSecond = second.primaryFor(key);
                assertEquals(nodeFirst, nodeSecond);
            }
        }
    }

    @Test
    void consistencyWithReplicasFactor() {
        for (final String node : nodes) {
            final Topology<String> first = createTopology(node);
            final Topology<String> second = createTopology(node);
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final Set<String> nodesFirst = first.replicasFor(key, RF);
                final Set<String> nodesSecond = second.replicasFor(key, RF);
                assertEquals(nodesFirst, nodesSecond);
            }
        }
    }

    @Test
    void distribution() {
        for (final String node : nodes) {
            final Topology<String> topology = createTopology(node);
            final Map<String, Integer> counters = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodeForKey = topology.primaryFor(key);
                counters.compute(nodeForKey, (n, c) -> c == null ? 1 : c + 1);
            }
            final int expectedKeysPerNode = (KEYS / getClusterSize());
            final int keysDelta = (int) (expectedKeysPerNode * DELTA);

            counters.values().forEach(i -> {
                final int diff = Math.abs(expectedKeysPerNode - i);
                final int maximumDeviation = expectedKeysPerNode * keysDelta;
                assertTrue(diff < maximumDeviation);
            });
        }
    }

    @Test
    void distributionWithReplicasFactor() {
        for (final String node : nodes) {
            final Topology<String> topology = createTopology(node);
            final Map<String, Integer> counters = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final Set<String> nodesForKey = topology.replicasFor(key, RF);
                nodesForKey.forEach(nodeForKey -> counters.compute(nodeForKey, (n, c) -> c == null ? 1 : c + 1));
            }
            final int expectedKeysPerNode = (KEYS / getClusterSize()) * RF.getFrom();
            final int keysDelta = (int) (expectedKeysPerNode * DELTA);

            counters.values().forEach(i -> {
                final int diff = Math.abs(expectedKeysPerNode - i);
                final int maximumDeviation = expectedKeysPerNode * keysDelta;
                assertTrue(diff < maximumDeviation);
            });
        }
    }

    private Topology<String> createTopology(@NotNull final String node) {
        return new ConsistentHashingTopology<>(Set.of(nodes), node, VNODES);
    }

}
