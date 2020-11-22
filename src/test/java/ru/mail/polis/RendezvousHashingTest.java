package ru.mail.polis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.kovalkov.sharding.RendezvousHashingImpl;

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
    private static final int[] TOPOLOGY_SIZES = {3, 5, 8, 10, 20};
    private static final int COUNT_OF_KEYS = 10_000_000;
    private static final int MAX_PORT = 65536;
    private static final int MIN_PORT = 0;
    private static final String URL = "http://localhost:";
    private static final String CURRENT_NODE = URL + MAX_PORT;

    @ParameterizedTest
    @MethodSource("nodesProvider")
    public void normalDistributionTest(@NotNull final Set<String> allNodes) {
        final Multiset<String> resultOfShardingIdentify = getResultOfIdentifies(getTopologyInstance(allNodes));
        assertTrue(isEquitableDistributionForSharding(resultOfShardingIdentify, allNodes.size()));
    }

    private static boolean isEquitableDistributionForSharding(@NotNull final Multiset<String> resultIdentify,
                                                              final int size) {
        final List<Integer> countsOfIndent = new ArrayList<>();
        resultIdentify.entrySet().iterator().forEachRemaining(i -> countsOfIndent.add(i.getCount()));
        final int avg = COUNT_OF_KEYS / size;
        final double sumOfDeviation = countsOfIndent.stream().map(i -> Math.pow((i - avg), 2))
                .reduce((double) 0, Double::sum);
        final double RMSDeviation = (int) Math.sqrt(sumOfDeviation/size-1);
        for (final int i : countsOfIndent) {
            if (i > avg + RMSDeviation*3 || i < avg - RMSDeviation*3) {
                return false;
            }
        }
        return true;
    }

    private static Multiset<String> getResultOfIdentifies(@NotNull final Topology<String> topology) {
        final Random random = new Random();
        final byte[] rndKey = new byte[50];
        final Multiset<String> identifyResult = HashMultiset.create();
        for (int i = 0; i < COUNT_OF_KEYS; i++) {
            random.nextBytes(rndKey);
            identifyResult.add(topology.identifyByKey(rndKey));
        }
        return identifyResult;
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
