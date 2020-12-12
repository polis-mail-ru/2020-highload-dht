package ru.mail.polis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.nik27090.RendezvousTopology;
import ru.mail.polis.service.nik27090.Topology;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RendezvousHashingTest {
    private static final int KEYS_COUNT = 100_000;
    private static final int KEY_LEN = 50;
    private static final double ERROR = 0.1;
    private static final String LOCALHOST = "http://localhost:";
    private static final String CURRENT_NODE = "http://localhost:65536";

    @Test
    void distributionTest() {
        Assertions.assertAll(
                () -> distributionTest(5),
                () -> distributionTest(10),
                () -> distributionTest(25),
                () -> distributionTest(50),
                () -> distributionTest(100)
        );
    }

    @Test
    void changeResistantTest() {
        final Set<String> nodes = randomNodes(5);
        final Topology<String> oldCluster = new RendezvousTopology(nodes, CURRENT_NODE, null);
        nodes.add(randomNode());
        final Topology<String> newCluster = new RendezvousTopology(nodes, CURRENT_NODE, null);
        int countKeysForNewNode = 0;
        for (int i = 0; i < KEYS_COUNT; i++) {
            final ByteBuffer key = randomKey();
            final String oldNode = oldCluster.getReplicas(key, 1)[0];
            final String newNode = newCluster.getReplicas(key, 1)[0];
            if (!oldNode.equals(newNode)) {
                countKeysForNewNode++;
            }
        }
        Assertions.assertTrue(countKeysForNewNode <= KEYS_COUNT / oldCluster.size());
    }

    public void distributionTest(final int sizeOfCluster) {
        final int replicationFactor = (sizeOfCluster / 2) + 1;
        final Set<String> nodes = randomNodes(sizeOfCluster);
        final Topology<String> cluster = createTopology(nodes);

        final Map<String, Integer> repeatReplicas = new HashMap<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            final String[] replicas = cluster.getReplicas(randomKey(), replicationFactor);
            for (String node : replicas) {
                int count = repeatReplicas.getOrDefault(node, 0);
                repeatReplicas.put(node, count + 1);
            }
        }

        for (String node : nodes) {
            final int countReplicas = repeatReplicas.get(node);
            final float countKeysForOneNode = (float) (KEYS_COUNT * replicationFactor) / sizeOfCluster;
            Assertions.assertTrue(
                    (countReplicas < (countKeysForOneNode + (countKeysForOneNode * ERROR)))
                            && (countReplicas > (countKeysForOneNode - (countKeysForOneNode * ERROR)))
            );
        }
    }

    private Set<String> randomNodes(int sizeOfCluster) {
        final Set<String> nodes = new HashSet<>();
        for (int i = 0; i < sizeOfCluster - 1; i++) {
            nodes.add(randomNode());
        }
        nodes.add(CURRENT_NODE);
        return nodes;
    }

    private String randomNode() {
        final int port = ThreadLocalRandom.current().nextInt(0, 65536);
        return LOCALHOST + port;
    }

    private ByteBuffer randomKey() {
        final Random r = new Random();
        byte[] bytes = new byte[KEY_LEN];
        r.nextBytes(bytes);
        return ByteBuffer.wrap(bytes);
    }

    private Topology<String> createTopology(Set<String> nodes) {
        return new RendezvousTopology(nodes, CURRENT_NODE, null);
    }
}
