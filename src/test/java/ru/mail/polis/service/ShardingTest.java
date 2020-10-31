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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for a sharded two node {@link Service} cluster.
 *
 * @author Vadim Tsesko
 */
class ShardingTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private int port0;
    private int port1;
    private File data0;
    private File data1;
    private DAO dao0;
    private DAO dao1;
    private Service storage0;
    private Service storage1;

    @BeforeEach
    void beforeEach() throws Exception {
        port0 = randomPort();
        port1 = randomPort();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        data0 = Files.createTempDirectory();
        dao0 = DAOFactory.create(data0);
        storage0 = ServiceFactory.create(port0, dao0, endpoints);
        storage0.start();
        data1 = Files.createTempDirectory();
        dao1 = DAOFactory.create(data1);
        storage1 = ServiceFactory.create(port1, dao1, endpoints);
        start(1, storage1);
    }

    @AfterEach
    void afterEach() throws IOException {
        stop(0, storage0);
        dao0.close();
        Files.recursiveDelete(data0);
        stop(1, storage1);
        dao1.close();
        Files.recursiveDelete(data1);
        endpoints = Collections.emptySet();
    }

    @Test
    void insert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = "key";
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value).getStatus());

            // Check
            final Response response0 = get(0, key);
            assertEquals(200, response0.getStatus());
            assertArrayEquals(value, response0.getBody());
            final Response response1 = get(1, key);
            assertEquals(200, response1.getStatus());
            assertArrayEquals(value, response1.getBody());
        });
    }

    @Test
    void insertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = new byte[0];

            // Insert
            assertEquals(201, upsert(0, key, value).getStatus());

            // Check
            final Response response0 = get(0, key);
            assertEquals(200, response0.getStatus());
            assertArrayEquals(value, response0.getBody());
            final Response response1 = get(1, key);
            assertEquals(200, response1.getStatus());
            assertArrayEquals(value, response1.getBody());
        });
    }

    @Test
    void lifecycle2keys() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key1 = randomId();
            final byte[] value1 = randomValue();
            final String key2 = randomId();
            final byte[] value2 = randomValue();

            // Insert 1
            assertEquals(201, upsert(0, key1, value1).getStatus());

            // Check
            assertArrayEquals(value1, get(0, key1).getBody());
            assertArrayEquals(value1, get(1, key1).getBody());

            // Insert 2
            assertEquals(201, upsert(1, key2, value2).getStatus());

            // Check
            assertArrayEquals(value1, get(0, key1).getBody());
            assertArrayEquals(value2, get(0, key2).getBody());
            assertArrayEquals(value1, get(1, key1).getBody());
            assertArrayEquals(value2, get(1, key2).getBody());

            // Delete 1
            assertEquals(202, delete(0, key1).getStatus());
            assertEquals(202, delete(1, key1).getStatus());

            // Check
            assertEquals(404, get(0, key1).getStatus());
            assertArrayEquals(value2, get(0, key2).getBody());
            assertEquals(404, get(1, key1).getStatus());
            assertArrayEquals(value2, get(1, key2).getBody());

            // Delete 2
            assertEquals(202, delete(0, key2).getStatus());
            assertEquals(202, delete(1, key2).getStatus());

            // Check
            assertEquals(404, get(0, key2).getStatus());
            assertEquals(404, get(1, key2).getStatus());
        });
    }

    @Test
    void upsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();

            // Insert value1
            assertEquals(201, upsert(0, key, value1).getStatus());

            // Insert value2
            assertEquals(201, upsert(1, key, value2).getStatus());

            // Check value 2
            final Response response0 = get(0, key);
            assertEquals(200, response0.getStatus());
            assertArrayEquals(value2, response0.getBody());
            final Response response1 = get(1, key);
            assertEquals(200, response1.getStatus());
            assertArrayEquals(value2, response1.getBody());
        });
    }

    @Test
    void upsertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final byte[] empty = new byte[0];

            // Insert value
            assertEquals(201, upsert(0, key, value).getStatus());

            // Insert empty
            assertEquals(201, upsert(0, key, empty).getStatus());

            // Check empty
            final Response response0 = get(0, key);
            assertEquals(200, response0.getStatus());
            assertArrayEquals(empty, response0.getBody());
            final Response response1 = get(1, key);
            assertEquals(200, response1.getStatus());
            assertArrayEquals(empty, response1.getBody());
        });
    }

    @Test
    void delete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value).getStatus());
            assertEquals(201, upsert(1, key, value).getStatus());

            // Delete
            assertEquals(202, delete(0, key).getStatus());

            // Check
            assertEquals(404, get(0, key).getStatus());
            assertEquals(404, get(1, key).getStatus());
        });
    }

    @Test
    void distribute() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, value, 1, 1).getStatus());
            assertEquals(201, upsert(1, key, value, 1, 1).getStatus());

            int copies = 0;

            // Stop node 0
            stop(0, storage0);

            // Check
            if (get(1, key, 1, 1).getStatus() == 200) {
                copies++;
            }

            // Start node 0
            storage0 = ServiceFactory.create(port0, dao0, endpoints);
            start(0, storage0);

            // Stop node 1
            stop(1, storage1);

            // Check
            if (get(0, key, 1, 1).getStatus() == 200) {
                copies++;
            }

            // Start node 1
            storage1 = ServiceFactory.create(port1, dao1, endpoints);
            start(1, storage1);

            // Check
            assertEquals(1, copies);
        });
    }
}
