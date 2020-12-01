package ru.mail.polis.service;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.kate.moreva.ConsistentTopology;
import ru.mail.polis.service.kate.moreva.Replicas;
import ru.mail.polis.service.kate.moreva.Topology;

public class ConsistentTopologyTest extends ClusterTestBase {
    private static final String ME = "http://localhost:8080";
    private static final Set<String> NODES = Set.of(ME, "http://localhost:8081", "http://localhost:8082");
    private static final int VNODE_COUNT = 200;
    private static final int KEYS_COUNT = 10_000_000;
    private static final int EXPECTED_KEYS_PER_NODE = KEYS_COUNT / NODES.size();
    private static final int KEYS_DELTA = (int) (EXPECTED_KEYS_PER_NODE * 0.1);

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
        final Topology<String> topology = createTopology();
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
                                Assertions.assertTrue(delta < KEYS_DELTA,
                                        "Node keys counter is out of range");
                            }
                    );
                });
    }

    private static Topology<String> createTopology() {
        return new ConsistentTopology(NODES, ME, VNODE_COUNT);
    }

    @Override
    int getClusterSize() {
        return 3;
    }
}
