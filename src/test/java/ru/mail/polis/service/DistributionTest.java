package ru.mail.polis.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static final int KEYS_COUNT = 300;

    @Override
    int getClusterSize() {
        return 3;
    }

    @Test
    void randomKeysDistribution() {
        // Random keys of the same length
        Collection<String> keys = new HashSet<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(randomId());
        }
        distributionUniformityCheck(keys);
    }

    @Test
    void randomKeysWithPostfixDistribution() {
        // Simulation of random keys generated from an email template
        Collection<String> keys = new HashSet<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(randomId() + "@mail.ru");
        }
        distributionUniformityCheck(keys);
    }

    @Test
    void numKeysDistribution() {
        // Hash check on numeric keys
        Collection<String> keys = new HashSet<>();
        for (int i = 0; i < KEYS_COUNT; i++) {
            keys.add(Integer.toString(i));
        }
        distributionUniformityCheck(keys);
    }

    private void distributionUniformityCheck(Collection<String> keys) {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final byte[] value = randomValue();
            stop(1);
            stop(2);

            int countNode0 = this.countKeysFromNode(keys, value, 0);

            stop(0);
            createAndStart(1);

            int countNode1 = this.countKeysFromNode(keys, value, 1);

            stop(1);
            createAndStart(2);
            int countNode2 = this.countKeysFromNode(keys, value, 2);

            // Allowable difference in the number of keys on nodes = 20%
            final int diff = KEYS_COUNT / 5;
            final String mess = "Keys on node 0: " + countNode0 + " node 1: " + countNode1 + " node 2: " + countNode2;
            assertEquals(keys.size(), countNode0 + countNode1 + countNode2);
            assertTrue(Math.abs(countNode0 - countNode1) < diff, mess);
            assertTrue(Math.abs(countNode1 - countNode2) < diff, mess);
            assertTrue(Math.abs(countNode0 - countNode2) < diff, mess);
        });
    }

    private int countKeysFromNode(Collection<String> keys, byte[] value, int node) {
        AtomicInteger countNode = new AtomicInteger();
        assertTimeoutPreemptively(TIMEOUT, () -> {
            for (String key : keys) {
                if (upsert(node, key, value, 1, 1).getStatus() == 201) {
                    countNode.getAndIncrement();
                }
            }
        });
        return countNode.get();
    }
}

