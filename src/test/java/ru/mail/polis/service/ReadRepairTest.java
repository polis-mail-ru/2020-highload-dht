package ru.mail.polis.service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.ClusterTestBase;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class ReadRepairTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    @Override
    int getClusterSize() {
        return 3;
    }

    /**
     * Test for read-repair functionality.
     * Checks whether ack nodes repairs the value if
     * the value is old.
     */
    @RepeatedTest(10)
    public void threeNodeTest() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int ackCount = 0;
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();
            assertEquals(201, upsert(0, key, value1, 2, 2).getStatus());
            assertEquals(201, upsert(0, key, value2, 1, 1).getStatus());
            checkResponse(200, value2, get(0, key, 2, 2));
            for (int node = 0; node < getClusterSize(); node++) {
                // Reinitialize cluster
                restartAllNodes();

                // Stop all nodes except for the current one
                for (int i = 0; i < getClusterSize(); i++) {
                    if (i != node) {
                        stop(i);
                    }
                }

                // Check
                final var resp = get(node, key, 1, 3);
                if (resp.getStatus() == 200 && Arrays.equals(value2, resp.getBody())) {
                    ackCount++;
                }

                // Help implementors with ms precision for conflict resolution
                waitForVersionAdvancement();
            }
            assertEquals(2, ackCount);
        });
    }
}
