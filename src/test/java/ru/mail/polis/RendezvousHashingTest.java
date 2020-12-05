package ru.mail.polis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
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
    private static final int SIZE_OF_RANDOM_KEY = 50;
    private static final int REPLICAS = 5;
    private static final int[] TOPOLOGY_SIZES = {3, 5, 8, 10, 20, 100};
    private static final int COUNT_OF_KEYS = 1_000_000;
    public static final double ERR = 0.05;
    private static final int MAX_PORT = 65536;
    private static final int MIN_PORT = 0;
    private static final String URL = "http://localhost:";
    private static final String CURRENT_NODE = URL + MAX_PORT;
    private static final Random random = new Random();

    @Test
    public void clusterChangeResistanceTest() {
        final Set<String> nodes = getRandomNodes(10);
        int acceptableMigration = COUNT_OF_KEYS / nodes.size();
        acceptableMigration += acceptableMigration * ERR;
        final Topology<String> topologyOld = getTopologyInstance(nodes);
        final String newNode = getRandomNode();
        nodes.add(newNode);
        final Topology<String> topologyNew = getTopologyInstance(nodes);
        final byte[][] keyArr = generateRandomKeys(COUNT_OF_KEYS);
        int countOfMigrateKey = 0;
        for (final byte[] bytes : keyArr) {
            final String before = topologyOld.replicasFor(ByteBuffer.wrap(bytes), 1)[0];
            final String after = topologyNew.replicasFor(ByteBuffer.wrap(bytes), 1)[0];
            if (!before.equals(after)) {
                countOfMigrateKey++;
            }
            if (countOfMigrateKey > acceptableMigration) break;
        }
        assertTrue(countOfMigrateKey <= acceptableMigration);
    }

    private static int compareOwners(@NotNull final List<String> before,
                                     @NotNull final List<String> after) {
        for (int i = 0; i < before.size(); i++) {
            for (int j = 0; j < after.size(); j++) {
                if (before.get(i).equals(after.get(j))) {
                    before.remove(i);
                    after.remove(j);
                }
            }
        }
        return before.size();
    }

    @ParameterizedTest
    @MethodSource("nodesProvider")
    public void uniformDistributionTest(@NotNull final Set<String> allNodes) {
        final Topology<String> topology = getTopologyInstance(allNodes);
        final Multiset<String> resultOfReplication = HashMultiset.create();
        final int replicas = (allNodes.size() / 2) + 1 ;
        System.out.println(replicas);
        final byte[] rndKey = new byte[SIZE_OF_RANDOM_KEY];
        for (int i = 0; i < COUNT_OF_KEYS; i++) {
            random.nextBytes(rndKey);
            final String[] owners = topology.replicasFor(ByteBuffer.wrap(rndKey),replicas);
            resultOfReplication.addAll(Arrays.asList(owners));
        }
        assertTrue(isUniformDistribution(resultOfReplication, allNodes.size()));
    }

    private static boolean isUniformDistribution(@NotNull final Multiset<String> resultOfReplicasFor,
                                                 final int size) {
        final List<Integer> countsOfIndent = new ArrayList<>();
        resultOfReplicasFor.entrySet().iterator().forEachRemaining(i -> countsOfIndent.add(i.getCount()));
        final long avg = resultOfReplicasFor.size() / size;
        return countsOfIndent.stream()
                .noneMatch(i -> i > avg + avg * ERR || i < avg - avg * ERR);
    }

    @NotNull
    private static RendezvousHashingImpl getTopologyInstance(@NotNull final Set<String> allNodes) {
        allNodes.add(CURRENT_NODE);
        return new RendezvousHashingImpl(CURRENT_NODE, allNodes);
    }

    @NotNull
    private static byte[][] generateRandomKeys(final int size) {
        final byte[][] rndKey = new byte[size][SIZE_OF_RANDOM_KEY];
        for (final byte[] bytes : rndKey) {
            random.nextBytes(bytes);
        }
        return rndKey;
    }

    @NotNull
    private static Stream<Set<String>> nodesProvider() {
        return Arrays.stream(TOPOLOGY_SIZES).mapToObj(RendezvousHashingTest::getRandomNodes);
    }

    @NotNull
    private static Set<String> getRandomNodes(final int size) {
        return random.ints(size-1, MIN_PORT, MAX_PORT - 1)
                .mapToObj(i -> URL + i).collect(Collectors.toSet());
    }

    @NotNull
    private static String getRandomNode() {
        return URL + (random.nextInt(MAX_PORT - 1) & Integer.MAX_VALUE);
    }
}
