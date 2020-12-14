package ru.mail.polis;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.basta123.Topology;
import ru.mail.polis.service.basta123.RendezvousTopology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RendezvousHashTest {

    private static final Random random = new Random();
    private static final int COUNT_OF_KEYS = 500000;
    private static final String URL = "http://localhost:";
    private static final int MAX_PORT = 65536;
    public static final double ALLOWABLE_ERROR = 0.3;

    @Test
    public void evenDistribution() {
        final int[] numberOfNodes = {5, 10, 25, 50, 100};
        for (int i = 0; i < numberOfNodes.length; i++) {
            Set<String> randomNodes = getRandomNodes(numberOfNodes[i]);
            final Topology<String> topology = new RendezvousTopology(randomNodes,
                    (String) randomNodes.toArray()[0]);
            Map<String, Integer> numberOfEachNode = new HashMap<>();
            for (int j = 0; j < COUNT_OF_KEYS; j++) {
                final List<String> repNodes = topology.getNodesForKey(ByteBuffer.wrap(getKey()), randomNodes.size() / 2);
                for (final String node : repNodes) {
                    if (numberOfEachNode.get(node) == null) {
                        numberOfEachNode.put(node, 1);
                    } else {
                        numberOfEachNode.put(node, (numberOfEachNode.get(node) + 1));
                    }

                }

            }
            int numberOfAllNodes = 0;
            for (final Map.Entry<String, Integer> node : numberOfEachNode.entrySet()) {
                numberOfAllNodes += node.getValue();
            }
            final int average = numberOfAllNodes / randomNodes.size();
            assertTrue(assertEvenDistribution(average, numberOfEachNode));
        }
    }

    private static boolean assertEvenDistribution(final int average, @NotNull final Map<String, Integer> numberOfEachNode) {
        final double max = average + average * ALLOWABLE_ERROR;
        final double min = average - average * ALLOWABLE_ERROR;
        for (final Map.Entry<String, Integer> node : numberOfEachNode.entrySet()) {
            final int numberOfNodes = node.getValue();
            if (numberOfNodes > max || numberOfNodes < min) {
                return false;
            }
        }
        return true;

    }

    @NotNull
    private static Set<String> getRandomNodes(final int size) {
        Set<String> randomNodes = new HashSet<>();
        while (randomNodes.size() < size) {
            int randomPort = random.nextInt(MAX_PORT - 1);
            randomNodes.add(URL + randomPort);
        }

        return randomNodes;
    }

    @NotNull
    private static byte[] getKey() {
        byte[] bytes = new byte[100];
        random.nextBytes(bytes);
        return bytes;
    }
}
