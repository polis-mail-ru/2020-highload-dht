package ru.mail.polis.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.gogun.ConsistentHashing;
import ru.mail.polis.service.gogun.Hashing;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopologyTests extends ClusterTestBase {

    private static final int VNODES = 100;
    private static final int KEYS = 10_000;

    @Override
    int getClusterSize() {
        return 15;
    }

    //тест проверяет детерминированность нод для репликации
    @Test
    void nodesForReplTest() {
        for (final String node : nodes) {
            final Hashing<String> TopologyFirst = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            final Hashing<String> TopologySecond = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final Set<String> nodesFirst = TopologyFirst.primaryFor(key, 3);
                final Set<String> nodesSecond = TopologySecond.primaryFor(key, 3);
                assertEquals(nodesFirst, nodesSecond);
            }
        }
    }

    //тест проверяет равномерность распределения ключей по нодам
    @Test
    void distributionTest() {
        for (final String node : nodes) {
            final ConsistentHashing topology = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            final Map<String, Integer> distr = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodeForKey = topology.primaryFor(key, 1).toArray(new String[0])[0];
                if (!distr.containsKey(nodeForKey)) {
                    distr.put(nodeForKey, 0);
                }

                distr.put(nodeForKey, distr.get(nodeForKey) + 1);
            }
            final int avg = distr
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum()
                    / distr.size();

            for (int i : distr.values()) {
                int diff = Math.abs(avg - i);
                float maximumDeviation = avg * 0.4f;
                Assertions.assertTrue(diff < maximumDeviation);
            }
        }
    }

    //тест проверяет, что запросы идут на все ноды
    @Test
    void multipleNodesTest() {
        for (final String node : nodes) {
            final ConsistentHashing topology = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            final Map<String, Integer> distr = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final String nodeForKey = topology.primaryFor(key, 1).toArray(new String[0])[0];
                if (!distr.containsKey(nodeForKey)) {
                    distr.put(nodeForKey, 0);
                }

                distr.put(nodeForKey, distr.get(nodeForKey) + 1);
            }

            for (int i : distr.values()) {
                Assertions.assertTrue(i != 0);
            }
        }
    }

    //тест проверяет распределение ключей по нодам при репликейшн факторе больше 1
    @Test
    void distributionTestMany() {
        for (final String node : nodes) {
            final ConsistentHashing topology = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            final Map<String, Integer> distr = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final Set<String> nodesForKey = topology.primaryFor(key, 5);
                for (final String nodeForKey : nodesForKey) {
                    if (!distr.containsKey(nodeForKey)) {
                        distr.put(nodeForKey, 0);
                    }

                    distr.put(nodeForKey, distr.get(nodeForKey) + 1);
                }
            }
            final int avg = distr
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum()
                    / distr.size();

            for (int i : distr.values()) {
                int diff = Math.abs(avg - i);
                float maximumDeviation = avg * 0.4f;
                Assertions.assertTrue(diff < maximumDeviation);
            }
        }
    }
}
