package ru.mail.polis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Files;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static final int KEYS_COUNT = 1000;
    private int port0;
    private int port1;
    private int port2;
    private File data0;
    private File data1;
    private File data2;
    private DAO dao0;
    private DAO dao1;
    private DAO dao2;
    private Service storage0;
    private Service storage1;
    private Service storage2;

    @BeforeEach
    void beforeEach() throws Exception {
        port0 = randomPort();
        port1 = randomPort();
        port2 = randomPort();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1), endpoint(port2)));
        data0 = Files.createTempDirectory();
        data1 = Files.createTempDirectory();
        data2 = Files.createTempDirectory();
        dao0 = DAOFactory.create(data0);
        dao1 = DAOFactory.create(data1);
        dao2 = DAOFactory.create(data2);
        storage0 = ServiceFactory.create(port0, dao0, endpoints);
        storage0.start();
        storage1 = ServiceFactory.create(port1, dao1, endpoints);
        storage1.start();
        storage2 = ServiceFactory.create(port2, dao2, endpoints);
        start(2, storage2);
    }

    @AfterEach
    void afterEach() throws IOException {
        stop(0, storage0);
        dao0.close();
        Files.recursiveDelete(data0);
        stop(1, storage1);
        dao1.close();
        Files.recursiveDelete(data1);
        stop(2, storage2);
        dao2.close();
        Files.recursiveDelete(data2);
        endpoints = Collections.emptySet();
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
            stop(1, storage1);
            stop(2, storage2);

            int countNode0 = this.countKeysFromNode(keys, value, 0);

            stop(0, storage0);
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            int countNode1 = this.countKeysFromNode(keys, value, 1);

            stop(1, storage1);
            storage2 = ServiceFactory.create(port2, dao2, endpoints);
            start(2, storage2);
            int countNode2 = this.countKeysFromNode(keys, value, 2);

            // Allowable difference in the number of keys on nodes = 10%
            final int diff = KEYS_COUNT / 10;
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

