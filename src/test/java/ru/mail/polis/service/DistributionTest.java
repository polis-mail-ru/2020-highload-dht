package ru.mail.polis.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.dao.suhova.RendezvousTopology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DistributionTest extends ClusterTestBase {
    private static final int KEYS_COUNT = 1000;

    @Override
    int getClusterSize() {
        return 5;
    }

    @Test
    void randomKeysDistribution() {
        // Random keys of the same length
        final Collection<String> keys = new ArrayList<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(randomId());
        }
        Assertions.assertAll(
            () -> checkDistribution(1, keys),
            () -> checkDistribution(2, keys),
            () -> checkDistribution(3, keys),
            () -> checkDistribution(4, keys)
        );
    }

    @Test
    void randomKeysWithPostfixDistribution() {
        // Simulation of random keys generated from an email template
        final Collection<String> keys = new ArrayList<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(randomId() + "@mail.ru");
        }
        Assertions.assertAll(
            () -> checkDistribution(1, keys),
            () -> checkDistribution(2, keys),
            () -> checkDistribution(3, keys),
            () -> checkDistribution(4, keys)
        );
    }

    @Test
    void numKeysDistribution() {
        // Hash check on numeric keys
        final Collection<String> keys = new ArrayList<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(Integer.toString(i));
        }
        Assertions.assertAll(
            () -> checkDistribution(1, keys),
            () -> checkDistribution(2, keys),
            () -> checkDistribution(3, keys),
            () -> checkDistribution(4, keys)
        );
    }

    @Test
    void addNodeToTopology() {
        final Set<String> topologySet = new HashSet<>();
        String me = "";
        for (int i = 0; i < getClusterSize(); i++) {
            String node = endpoint(randomPort());
            topologySet.add(node);
            me = node;
        }
        RendezvousTopology topology = new RendezvousTopology(topologySet, me);

        final Collection<String> keys = new ArrayList<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(randomId());
        }

        final Map<String, List<String>> keysByNodes = new HashMap<>();
        for (String key : keys) {
            String[] nodes = topology.getNodesByKey(key, 1);
            for (String node : nodes) {
                if (keysByNodes.containsKey(node)) {
                    keysByNodes.get(node).add(key);
                } else {
                    final List<String> list = new ArrayList<>();
                    list.add(key);
                    keysByNodes.put(node, list);
                }
            }
        }
        topologySet.add(endpoint(randomPort()));
        topology = new RendezvousTopology(topologySet, me);

        // Number of keys to move
        int count = 0;
        for (String key : keys) {
            String[] nodes = topology.getNodesByKey(key, 1);
            for (String node : nodes) {
                if (!keysByNodes.containsKey(node) || !keysByNodes.get(node).contains(key)) {
                    count++;
                }
            }
        }
        // Allowed number of keys that move when adding a new node = 20%
        Assertions.assertTrue(count < KEYS_COUNT / 5,
            "Number of keys transferred to another node: " + count);
    }

    void checkDistribution(int replicationFactor, final Collection<String> keysList) {
        final Map<String, Integer> keyCount = new HashMap<>();
        final RendezvousTopology topology = createTopology(getClusterSize());
        for (String key : keysList) {
            String[] nodes = topology.getNodesByKey(key, replicationFactor);
            for (String node : nodes) {
                if (keyCount.containsKey(node)) {
                    keyCount.put(node, keyCount.get(node) + 1);
                } else {
                    keyCount.put(node, 1);
                }
            }
        }
        final int min = keyCount.entrySet().stream().min(Map.Entry.comparingByValue()).get().getValue();
        final int max = keyCount.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
        final int diff = max - min;
        // Allowable difference in the number of keys on nodes = 10%
        Assertions.assertTrue(diff < KEYS_COUNT / 10,
            "Difference between keys count: " + diff);
    }

    private RendezvousTopology createTopology(int size) {
        final Set<String> topology = new HashSet<>();
        String me = "";
        for (int i = 0; i < size; i++) {
            String node = endpoint(randomPort());
            topology.add(node);
            me = node;
        }
        return new RendezvousTopology(topology, me);
    }
}

