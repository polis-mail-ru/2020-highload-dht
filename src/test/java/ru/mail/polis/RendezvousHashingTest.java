package ru.mail.polis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.kovalkov.sharding.RendezvousHashingImpl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.mail.polis.service.kovalkov.sharding.Topology;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class RendezvousHashingTest {
    private static final int[] TOPOLOGY_SIZES = {3, 5, 8, 10, 20, 100};
    private static final int COUNT_OF_KEYS = 1_000_000;
    public static final double ERR = 0.05;
    private static final int MAX_PORT = 65536;
    private static final int MIN_PORT = 0;
    private static final String URL = "http://localhost:";
    private static final String CURRENT_NODE = URL + MAX_PORT;

    @ParameterizedTest
    @MethodSource("nodesProvider")
    public void normalDistributionTest(@NotNull final Set<String> allNodes) {
        final Topology<String> topology = getTopologyInstance(allNodes);
        final Multiset<String> resultOfReplication = HashMultiset.create();
        final Random random = new Random();
        final int[] replicas = random.ints(COUNT_OF_KEYS, 1, (int) Math.ceil(allNodes.size() * 0.75) + 1).toArray();
        final byte[] rndKey = new byte[50];
        long countOFReplicas = 0;
        for (int i = 0; i < COUNT_OF_KEYS; i++) {
            random.nextBytes(rndKey);
            countOFReplicas += replicas[i];
            final String[] owners = topology.replicasFor(ByteBuffer.wrap(rndKey),replicas[i]);
            resultOfReplication.addAll(Arrays.asList(owners));
        }
        assertTrue(isEquitableDistribution(resultOfReplication, allNodes.size(), countOFReplicas));
    }

    private static boolean isEquitableDistribution(@NotNull final Multiset<String> resultIdentify,
                                                   final int size, final long replicas) {
        final List<Integer> countsOfIndent = new ArrayList<>();
        resultIdentify.entrySet().iterator().forEachRemaining(i -> countsOfIndent.add(i.getCount()));
        final long avg = replicas / size;
        final List<Integer> nonEquitableNodes = countsOfIndent.stream()
                .filter(i -> i > avg + avg * ERR || i < avg - avg * ERR).collect(Collectors.toList());
        return nonEquitableNodes.isEmpty();
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
