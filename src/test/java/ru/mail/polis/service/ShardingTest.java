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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for a sharded two node {@link Service} cluster.
 *
 * @author Vadim Tsesko
 */
class ShardingTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    @Override
    int getClusterSize() {
        return 2;
    }

    @Test
    void insert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = "key";
            final byte[] value = randomValue();

            for (int node = 0; node < getClusterSize(); node++) {
                // Insert
                assertEquals(201, upsert(node, key, NO_EXPIRE, value).getStatus());

                // Check
                for (int i = 0; i < getClusterSize(); i++) {
                    checkResponse(200, value, get(i, key, NO_EXPIRE));
                }
            }
        });
    }

    @Test
    void insertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = new byte[0];

            for (int node = 0; node < getClusterSize(); node++) {
                // Insert
                assertEquals(201, upsert(node, key, NO_EXPIRE, value).getStatus());

                // Check
                for (int i = 0; i < getClusterSize(); i++) {
                    checkResponse(200, value, get(i, key, NO_EXPIRE));
                }
            }
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
            assertEquals(201, upsert(0, key1, NO_EXPIRE, value1).getStatus());

            // Check
            assertArrayEquals(value1, get(0, key1, NO_EXPIRE).getBody());
            assertArrayEquals(value1, get(1, key1, NO_EXPIRE).getBody());

            // Insert 2
            assertEquals(201, upsert(1, key2, NO_EXPIRE, value2).getStatus());

            // Check
            assertArrayEquals(value1, get(0, key1, NO_EXPIRE).getBody());
            assertArrayEquals(value2, get(0, key2, NO_EXPIRE).getBody());
            assertArrayEquals(value1, get(1, key1, NO_EXPIRE).getBody());
            assertArrayEquals(value2, get(1, key2, NO_EXPIRE).getBody());

            // Delete 1
            assertEquals(202, delete(0, key1, NO_EXPIRE).getStatus());
            assertEquals(202, delete(1, key1, NO_EXPIRE).getStatus());

            // Check
            assertEquals(404, get(0, key1, NO_EXPIRE).getStatus());
            assertArrayEquals(value2, get(0, key2, NO_EXPIRE).getBody());
            assertEquals(404, get(1, key1, NO_EXPIRE).getStatus());
            assertArrayEquals(value2, get(1, key2, NO_EXPIRE).getBody());

            // Delete 2
            assertEquals(202, delete(0, key2, NO_EXPIRE).getStatus());
            assertEquals(202, delete(1, key2, NO_EXPIRE).getStatus());

            // Check
            assertEquals(404, get(0, key2, NO_EXPIRE).getStatus());
            assertEquals(404, get(1, key2, NO_EXPIRE).getStatus());
        });
    }

    @Test
    void upsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();

            // Insert value1
            assertEquals(201, upsert(0, key, NO_EXPIRE, value1).getStatus());

            // Insert value2
            assertEquals(201, upsert(1, key, NO_EXPIRE, value2).getStatus());

            // Check value 2
            for (int i = 0; i < getClusterSize(); i++) {
                checkResponse(200, value2, get(i, key, NO_EXPIRE));
            }
        });
    }

    @Test
    void upsertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final byte[] empty = new byte[0];

            // Insert value
            assertEquals(201, upsert(0, key, NO_EXPIRE, value).getStatus());

            // Insert empty
            assertEquals(201, upsert(0, key, NO_EXPIRE, empty).getStatus());

            // Check empty
            for (int i = 0; i < getClusterSize(); i++) {
                checkResponse(200, empty, get(i, key, NO_EXPIRE));
            }
        });
    }

    @Test
    void delete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, NO_EXPIRE, value).getStatus());
            assertEquals(201, upsert(1, key, NO_EXPIRE, value).getStatus());

            // Delete
            assertEquals(202, delete(0, key, NO_EXPIRE).getStatus());

            // Check
            assertEquals(404, get(0, key, NO_EXPIRE).getStatus());
            assertEquals(404, get(1, key, NO_EXPIRE).getStatus());
        });
    }

    @Test
    void distribute() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();

            // Insert
            assertEquals(201, upsert(0, key, NO_EXPIRE, value, 1, 1).getStatus());
            assertEquals(201, upsert(1, key, NO_EXPIRE, value, 1, 1).getStatus());

            // Stop all
            for (int node = 0; node < getClusterSize(); node++) {
                stop(node);
            }

            // Check each
            int copies = 0;
            for (int node = 0; node < getClusterSize(); node++) {
                // Start node
                createAndStart(node);

                // Check
                if (get(node, key, NO_EXPIRE, 1, 1).getStatus() == 200) {
                    copies++;
                }

                // Stop node 0
                stop(node);
            }
            assertEquals(1, copies);
        });
    }
}
