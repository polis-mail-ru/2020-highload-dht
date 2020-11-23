package ru.mail.polis.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mail.polis.TestBase;
import ru.mail.polis.service.alexander.marashov.topologies.RendezvousTopology;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class RendezvousTopologyTest extends TestBase {

    private static final long NODES_COUNT = 20;
    private static final long REQUESTS_COUNT = NODES_COUNT * 100000;
    private static Topology<String> topology;

    @BeforeAll
    static void beforeAll() {
        final Set<String> nodesSet = new TreeSet<>();
        final String local = "localhost:8000";
        nodesSet.add(local);
        for (int i = 1; i < NODES_COUNT; ++i) {
            nodesSet.add("localhost:" + (8000 + i));
        }
        topology = new RendezvousTopology(nodesSet, local);
    }

    @Test
    void singleNodeUniformDistribution() {
        final Map<String, Long> countersMap = new TreeMap<>();
        for (final String str: topology.all()) {
            countersMap.put(str, 0L);
        }

        for (int i = 0; i < REQUESTS_COUNT; ++i) {
            final ByteBuffer randomKey = randomBuffer(randomSize(200));
            final String node = topology.primaryFor(randomKey);
            countersMap.compute(node, (n, counter) -> counter + 1L);
        }
        countersMap.values().stream().sorted().forEach(v -> System.out.println(v + " "));
    }

    @Test
    void multipleNodesUniformDistribution() {
        final Map<String, Long> countersMap = new TreeMap<>();
        for (final String str: topology.all()) {
            countersMap.put(str, 0L);
        }

        for (int i = 0; i < REQUESTS_COUNT; ++i) {
            final ByteBuffer randomKey = randomBuffer(randomSize(200));
            final String[] nodes = topology.primariesFor(randomKey, topology.getQuorumCount());
            for (final String node: nodes) {
                countersMap.compute(node, (n, counter) -> counter + 1L);
            }
        }
        countersMap.values().stream().sorted().forEach(v -> System.out.println(v + " "));
    }
}
