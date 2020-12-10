package ru.mail.polis.service;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.kate.moreva.ConsistentTopology;
import ru.mail.polis.service.kate.moreva.Replicas;
import ru.mail.polis.service.kate.moreva.Topology;

public class ConsistentTopologyTest extends ClusterTestBase {
    private static final String ME = "http://localhost:8080";
    private final int VNODE_COUNT = 400;
    private final int KEYS_COUNT = 1_000_000;
    private final double POSSIBLE_ERROR = 0.1;

    @Test
    void nodesDeterminismTest() {
        final Topology<String> topology1 = createTopology();
        final Topology<String> topology2 = createTopology();
        final Topology<String> topology3 = createTopology();
        Replicas replicas = new Replicas(getClusterSize(), getClusterSize());
        Replicas replicas2 = new Replicas(2, getClusterSize());

        for (long i = 0; i < KEYS_COUNT; i++) {
            final ByteBuffer key = randomKeyBuffer();
            final String node1 = String.valueOf(topology1.primaryFor(key.duplicate(), replicas, replicas.getAck()));
            final String node2 = String.valueOf(topology2.primaryFor(key.duplicate(), replicas, replicas.getAck()));
            final String node3 = String.valueOf(topology3.primaryFor(key.duplicate(), replicas2, replicas.getAck()));
            Assertions.assertEquals(node1, node2);
            Assertions.assertEquals(node1, node3);
        }
    }

    @Test
    void distributionTest() {
        final Replicas replicas = new Replicas(3, 3);
        Set<String> set = new HashSet<>();
        Collections.addAll(set, nodes);
        final Topology<String> topology = new ConsistentTopology(set, ME, VNODE_COUNT);
        final Map<String, Integer> counters = new HashMap<>();
        for (long i = 0; i < KEYS_COUNT; i++) {
            final ByteBuffer key = randomKeyBuffer();
            final String node = String.valueOf(topology.primaryFor(key.duplicate(), replicas, replicas.getAck()));
            if (!counters.containsKey(node)) {
                counters.put(node, 0);
            }
            counters.put(node, counters.get(node) + 1);
        }

        int sum = 0;
        for (int f : counters.values()) {
            sum += f;
        }
        final int avg = sum / counters.size();
        counters.values()
                .forEach(e -> {
                    final int delta = Math.abs(e - avg);
                    Assertions.assertAll(() ->
                            {
                                Assertions.assertTrue(e != 0);
                                Assertions.assertTrue(delta < (KEYS_COUNT / nodes.length) * POSSIBLE_ERROR,
                                        "Node keys counter is out of range");
                            }
                    );
                });
    }

    @Test
    void afterAdditionTest() {
        final Replicas replicas = new Replicas(3, 3);
        Set<String> set = new HashSet<>();
        Collections.addAll(set, nodes);
        final Topology<String> topology = new ConsistentTopology(set, ME, VNODE_COUNT);
        final String extraNode =  "http://localhost:8085";
        set.add(extraNode);
        final Topology<String> topology1 = new ConsistentTopology(set, ME, 800);

        int mistakesCount = 0;
        for (int i = 0; i < KEYS_COUNT; i++) {
            final ByteBuffer key = randomKeyBuffer();
            final var original = topology.primaryFor(key.duplicate(), replicas, replicas.getAck());
            final var extended = topology1.primaryFor(key.duplicate(), replicas, replicas.getAck());
            mistakesCount += extended.stream().filter(it -> !original.contains(it)).count();
        }
        Assertions.assertTrue(mistakesCount / KEYS_COUNT < POSSIBLE_ERROR);
    }

    private Topology<String> createTopology() {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, nodes);
        return new ConsistentTopology(set, ME, VNODE_COUNT);
    }

    @Override
    int getClusterSize() {
        return 3;
    }
}
