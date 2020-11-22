package ru.mail.polis;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.kovalkov.sharding.RendezvousHashingImpl;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.mail.polis.service.kovalkov.sharding.Topology;

public class RendezvousHashingTest {
    private static final int[] TOPOLOGY_SIZES = {3, 5, 8, 10, 25, 50, 100, 500, 1000};
    private static final int COUNT_OF_KEYS = 100_000;
    private static final int MAX_PORT = 65536;
    private static final int MIN_PORT = 0;
    private static final String URL = "http://localhost:";
    private static final String CURRENT_NODE = URL + MAX_PORT;

    @ParameterizedTest
    @MethodSource("nodesProvider")
    public void normalDistributionTest(@NotNull final Set<String> allNodes) {
        final Topology<String> topology = getTopologyInstance(allNodes);
        System.out.println(Arrays.toString(topology.allNodes()));
        final Random random = new Random();
        for (int i = 0; i < COUNT_OF_KEYS; i++) {
        }
    }

    @NotNull
    private static RendezvousHashingImpl getTopologyInstance(@NotNull final Set<String> allNodes) {
        allNodes.add(CURRENT_NODE);
        return new RendezvousHashingImpl(CURRENT_NODE, allNodes);
    }

    @NotNull
    private static Stream<Set<String>> nodesProvider() {
        return Arrays.stream(TOPOLOGY_SIZES).mapToObj(RendezvousHashingTest::getSetOfRandomNode);
    }

    @NotNull
    private static Set<String> getSetOfRandomNode(final int size) {
        return new Random().ints(size-1, MIN_PORT, MAX_PORT - 1).
                mapToObj(i -> URL + i).collect(Collectors.toSet());
    }
}
