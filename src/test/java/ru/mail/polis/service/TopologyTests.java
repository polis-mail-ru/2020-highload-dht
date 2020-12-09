package ru.mail.polis.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.gogun.ConsistentHashing;
import ru.mail.polis.service.gogun.Hashing;
import ru.mail.polis.service.gogun.ServiceUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopologyTests extends ClusterTestBase {

    private static final int VNODES = 100;
    private static final int KEYS = 10_000;

    @Override
    int getClusterSize() {
        return 9;
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
            final int eachNodeNum = KEYS / nodes.length;

            for (int i : distr.values()) {
                int diff = Math.abs(eachNodeNum - i);
                float maximumDeviation = eachNodeNum * 0.4f;
                Assertions.assertTrue(diff < maximumDeviation);
            }
        }
    }

    //тест проверяет распределение ключей по нодам при репликейшн факторе больше 1
    @Test
    void distributionTestMany() {
        final int replicationFactor = 5;
        for (final String node : nodes) {
            final ConsistentHashing topology = new ConsistentHashing(Arrays.asList(nodes), node, VNODES);
            final Map<String, Integer> distr = new HashMap<>();
            for (int i = 0; i < KEYS; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final Set<String> nodesForKey = topology.primaryFor(key, replicationFactor);
                for (final String nodeForKey : nodesForKey) {
                    if (!distr.containsKey(nodeForKey)) {
                        distr.put(nodeForKey, 0);
                    }

                    distr.put(nodeForKey, distr.get(nodeForKey) + 1);
                }
            }

            final int eachNodeNum = KEYS / nodes.length * (replicationFactor);

            for (int i : distr.values()) {
                int diff = Math.abs(eachNodeNum - i);
                float maximumDeviation = eachNodeNum * 0.4f;
                Assertions.assertTrue(diff < maximumDeviation);
            }
        }
    }

    //тест проверяет, что при добавлении ноды в кластер не происходит полной миграции ключей
    @Test
    void addNodeTest() {
        final String nodeToAdd = nodes[nodes.length - 1];
        final List<String> mutableNodes = new ArrayList<>(Arrays.asList(nodes));
        mutableNodes.remove(nodeToAdd);
        final String node = nodes[0];
        final ConsistentHashing topologyNotfull = new ConsistentHashing(mutableNodes, node, VNODES);
        final Map<ByteBuffer, String> keyNodeNotFull = new HashMap<>();
        for (int i = 0; i < KEYS; i++) {
            final ByteBuffer key = ServiceUtils.getBuffer(new byte[]{(byte) i});
            final String nodeForKey = topologyNotfull.primaryFor(key, 1).toArray(new String[0])[0];
            keyNodeNotFull.put(key, nodeForKey);
        }

        mutableNodes.add(nodeToAdd);
        final ConsistentHashing topologyFull = new ConsistentHashing(mutableNodes, node, VNODES);
        final Map<ByteBuffer, String> keyNodeFull = new HashMap<>();
        for (int i = 0; i < KEYS; i++) {
            final ByteBuffer key = ServiceUtils.getBuffer(new byte[]{(byte) i});
            final String nodeForKey = topologyFull.primaryFor(key, 1).toArray(new String[0])[0];
            keyNodeFull.put(key, nodeForKey);
        }
        int migrationCounter = 0;
        for (int i = 0; i < KEYS; i++) {
            final String notFullNode = keyNodeNotFull.get(ServiceUtils.getBuffer(new byte[]{(byte) i}));
            final String fullNode = keyNodeFull.get(ServiceUtils.getBuffer(new byte[]{(byte) i}));
            if (!fullNode.equals(notFullNode)) {
                migrationCounter++;
            }
        }

        Assertions.assertTrue(migrationCounter < KEYS / 2);
    }
}
