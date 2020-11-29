package ru.mail.polis.expire.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class ShardingExpireTest extends ClusterExpireTestBase {
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
            final String expire = randomExpire(randomExpire());

            for (int node = 0; node < getClusterSize(); node++) {
                // Insert
                assertEquals(201, upsert(node, key, expire, value).getStatus());

                // Check
                for (int i = 0; i < getClusterSize(); i++) {
                    checkResponse(200, value, get(i, key, expire));
                }
            }
        });
    }

    @Test
    void insertExpire() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = "key";
            final byte[] value = randomValue();
            final String expire = randomExpire(randomExpire());

            for (int node = 0; node < getClusterSize(); node++) {
                // Insert
                assertEquals(201, upsert(node, key, expire, value).getStatus());

                // Check
                Thread.sleep(31000);
                for (int i = 0; i < getClusterSize(); i++) {
                    checkResponse(404, value, get(i, key, expire));
                }
            }
        });
    }

    @Test
    void upsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();
            final String expire1 = randomExpire(randomExpire());
            final String expire2 = randomExpire(randomExpire());

            // Insert value1
            assertEquals(201, upsert(0, key, expire1, value1).getStatus());

            // Insert value2
            assertEquals(201, upsert(1, key, expire2, value2).getStatus());

            // Check value 2
            for (int i = 0; i < getClusterSize(); i++) {
                checkResponse(200, value2, get(i, key, expire2));
            }
        });
    }

    @Test
    void upsertExpire() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();
            final String expire1 = randomExpire(randomExpire());
            final String expire2 = randomExpire(randomExpire());

            // Insert value1
            assertEquals(201, upsert(0, key, expire1, value1).getStatus());

            // Insert value2
            assertEquals(201, upsert(1, key, expire2, value2).getStatus());

            // Check value 2
            Thread.sleep(31000);
            for (int i = 0; i < getClusterSize(); i++) {
                checkResponse(404, value2, get(i, key, expire2));
            }
        });
    }

    @Test
    void delete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final String expire = randomExpire(randomExpire());

            // Insert
            assertEquals(201, upsert(0, key, expire, value).getStatus());
            assertEquals(201, upsert(1, key, expire, value).getStatus());

            // Delete
            assertEquals(202, delete(0, key, expire).getStatus());

            // Check
            assertEquals(404, get(0, key, expire).getStatus());
            assertEquals(404, get(1, key, expire).getStatus());
        });
    }

    @Test
    void deleteExpire() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final String expire = randomExpire(randomExpire());

            // Insert
            assertEquals(201, upsert(0, key, expire, value).getStatus());
            assertEquals(201, upsert(1, key, expire, value).getStatus());

            // Delete
            Thread.sleep(31000);
            assertEquals(404, delete(0, key, expire).getStatus());

        });
    }
}