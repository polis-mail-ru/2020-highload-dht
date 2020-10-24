/*
 * Copyright 2020 (c) Odnoklassniki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis.service;

import one.nio.http.Response;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for a three node replicated {@link Service} cluster.
 *
 * @author Vadim Tsesko
 */
class ThreeNodeTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
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
    void tooSmallRF() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            assertEquals(400, get(0, randomId(), 0, 3).getStatus());
            assertEquals(400, upsert(0, randomId(), randomValue(), 0, 3).getStatus());
            assertEquals(400, delete(0, randomId(), 0, 3).getStatus());
        });
    }

    @Test
    void tooBigRF() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            assertEquals(400, get(0, randomId(), 4, 3).getStatus());
            assertEquals(400, upsert(0, randomId(), randomValue(), 4, 3).getStatus());
            assertEquals(400, delete(0, randomId(), 4, 3).getStatus());
        });
    }

    @Test
    void unreachableRF() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            stop(0, storage0);
            assertEquals(504, get(1, randomId(), 3, 3).getStatus());
            assertEquals(504, upsert(1, randomId(), randomValue(), 3, 3).getStatus());
            assertEquals(504, delete(1, randomId(), 3, 3).getStatus());
        });
    }

    @Test
    void overlapRead() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 3).getStatus());

            // Check
            final Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());
        });
    }

    @Test
    void overlapWrite() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 3).getStatus());

            // Check
            final Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());
        });
    }

    @Test
    void overwrite() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();

            // Insert 1
            assertEquals(201, upsert(0, key, value1, 2, 3).getStatus());

            // Check 1
            Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value1, response.getBody());

            // Help implementors with second precision for conflict resolution
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));

            // Insert 2
            assertEquals(201, upsert(2, key, value2, 2, 3).getStatus());

            // Check 2
            response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value2, response.getBody());
        });
    }

    @Test
    void overlapDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 3).getStatus());

            // Check
            Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());

            // Delete
            assertEquals(202, delete(0, key, 2, 3).getStatus());

            // Check
            response = get(1, key, 2, 3);
            assertEquals(404, response.getStatus());
        });
    }

    @Test
    void missedWrite() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Stop node 1
            stop(1, storage1);

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 3).getStatus());

            // Start node 1
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check
            final Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());
        });
    }

    @Test
    void missedDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 3).getStatus());

            // Stop node 0
            stop(0, storage0);

            // Help implementors with second precision for conflict resolution
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));

            // Delete
            assertEquals(202, delete(1, key, 2, 3).getStatus());

            // Start node 0
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            start(0, storage0);

            // Check
            final Response response = get(0, key, 2, 3);
            assertEquals(404, response.getStatus());
        });
    }

    @Test
    void tolerateFailure() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert into node 2
            assertEquals(201, upsert(2, key, value, 2, 3).getStatus());

            // Stop node 2
            stop(2, storage2);

            // Check
            Response response = get(1, key, 2, 3);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());

            // Delete
            assertEquals(202, delete(0, key, 2, 3).getStatus());

            // Check
            response = get(1, key, 2, 3);
            assertEquals(404, response.getStatus());
        });
    }

    @Test
    void respectRF1() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 1, 1).getStatus());

            int copies = 0;

            // Stop all nodes
            stop(0, storage0);
            stop(1, storage1);
            stop(2, storage2);

            // Start node 0
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            start(0, storage0);

            // Check node 0
            if (get(0, key, 1, 1).getStatus() == 200) {
                copies++;
            }

            // Stop node 0
            stop(0, storage0);

            // Start node 1
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check node 1
            if (get(1, key, 1, 1).getStatus() == 200) {
                copies++;
            }

            // Stop node 1
            stop(1, storage1);

            // Start node 2
            storage2 = ServiceFactory.create(port2, dao2, endpoints);
            start(2, storage2);

            // Check node 2
            if (get(2, key, 1, 1).getStatus() == 200) {
                copies++;
            }

            // Start node 0 & 1
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            storage0.start();
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check
            assertEquals(1, copies);
        });
    }

    @Test
    void respectRF2() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 2, 2).getStatus());

            int copies = 0;

            // Stop all nodes
            stop(0, storage0);
            stop(1, storage1);
            stop(2, storage2);

            // Start node 0
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            start(0, storage0);

            // Check node 0
            if (get(0, key, 1, 2).getStatus() == 200) {
                copies++;
            }

            // Stop node 0
            stop(0, storage0);

            // Start node 1
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check node 1
            if (get(1, key, 1, 2).getStatus() == 200) {
                copies++;
            }

            // Stop node 1
            stop(1, storage1);

            // Start node 2
            storage2 = ServiceFactory.create(port2, dao2, endpoints);
            start(2, storage2);

            // Check node 2
            if (get(2, key, 1, 2).getStatus() == 200) {
                copies++;
            }

            // Start node 0 & 1
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            storage0.start();
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check
            assertEquals(2, copies);
        });
    }
}
