package ru.mail.polis.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.mail.polis.TestBase;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;
import ru.mail.polis.service.alexander.marashov.topologies.TopologyFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TopologyTest extends TestBase {

    private static final String TOO_BIG_DELTA_ERROR = "Too big delta between min and max values: " +
            "allowed delta: %d, actual delta is %d.";
    private static final long REQUESTS_COUNT = 1000000;
    private static Topology<String> topology;

    static void beforeAll(int nodesCount) {
        final Set<String> nodesSet = new TreeSet<>();
        final String local = randomAddress();
        nodesSet.add(local);
        for (int i = 1; i < nodesCount; i++) {
            nodesSet.add(randomAddress());
        }
        topology = TopologyFactory.create(nodesSet, local);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 10, 100})
    void singleNodeUniformDistribution(final int nodesCount) {
        beforeAll(nodesCount);

        final Map<String, Long> countersMap = new TreeMap<>();
        for (int i = 0; i < REQUESTS_COUNT; i++) {
            final ByteBuffer randomKey = randomBuffer(randomInt(100));
            final String node = topology.primaryFor(randomKey);
            countersMap.merge(node, 1L, Long::sum);
        }
        final List<Long> list = countersMap
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        Assertions.assertEquals(nodesCount, list.size());
        countersMap.values().stream().sorted().forEach(v -> System.out.println(v + " "));
        final long minCount = list.get(0);
        final long maxCount = list.get(list.size() - 1);
        final long delta = maxCount - minCount;
        final long allowedDelta = (maxCount + minCount) / 2 / 10;
        Assertions.assertTrue(allowedDelta >= delta, String.format(TOO_BIG_DELTA_ERROR, allowedDelta, delta));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 10, 100})
    void multipleNodesUniformDistribution(final int nodesCount) {
        beforeAll(nodesCount);

        final Map<String, Long> countersMap = new TreeMap<>();

        for (int i = 0; i < REQUESTS_COUNT; i++) {
            final ByteBuffer randomKey = randomBuffer(randomInt(100));
            final String[] nodes = topology.primariesFor(randomKey, topology.getQuorumCount());
            for (final String node: nodes) {
                countersMap.merge(node, 1L, Long::sum);
            }
        }
        final List<Long> list = countersMap
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        Assertions.assertEquals(nodesCount, list.size());
        countersMap.values().stream().sorted().forEach(v -> System.out.println(v + " "));
        final long minCount = list.get(0);
        final long maxCount = list.get(list.size() - 1);
        final long delta = maxCount - minCount;
        final long allowedDelta = (maxCount + minCount) / 2 / 10;
        Assertions.assertTrue(allowedDelta >= delta, String.format(TOO_BIG_DELTA_ERROR, allowedDelta, delta));
    }
}
