package ru.mail.polis.service;

import one.nio.http.HttpClient;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Files;
import ru.mail.polis.TestBase;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class SingleNodeExpireTest extends TestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static File data;
    private static DAO dao;
    private static int port;
    private static Service storage;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() throws Exception {
        port = randomPort();
        data = Files.createTempDirectory();
        dao = DAOFactory.create(data);
        final String endpoint = endpoint(port);
        storage = ServiceFactory.create(port, dao, Collections.singleton(endpoint));
        storage.start();
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        reset();
    }

    @AfterAll
    static void afterAll() throws IOException {
        client.close();
        storage.stop();
        dao.close();
        Files.recursiveDelete(data);
    }

    private static void reset() {
        if (client != null) {
            client.close();
        }
        client = new HttpClient(
                new ConnectionString(
                        "http://localhost:" + port +
                                "?timeout=" + (TIMEOUT.toMillis() / 2)));
    }

    @NotNull
    private static String path(@NotNull final String id) {
        return "/v0/entity?id=" + id;
    }

    @NotNull
    private static String path(@NotNull final String id,
                               final long expire) {
        return "/v0/entity?id=" + id + "&expires=" + expire;
    }

    private Response get(@NotNull final String key) throws Exception {
        return client.get(path(key));
    }

    private Response delete(@NotNull final String key) throws Exception {
        return client.delete(path(key));
    }

    private Response upsert(
            @NotNull final String key,
            @NotNull final byte[] data,
            final long expire) throws Exception {
        return client.put(path(key, expire), data);
    }

    @Test
    void insert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final long expire = 1_000;

            // Insert
            assertEquals(201, upsert(key, value, expire).getStatus());

            // Check
            final Response response = get(key);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());
        });
    }


    @Test
    void deadInsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final long expire = 1_000;

            // Insert
            assertEquals(201, upsert(key, value, expire).getStatus());

            // Check
            final Response response = get(key);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value, response.getBody());

            Thread.sleep(1_000);
            final Response response2 = get(key);
            assertEquals(404, response2.getStatus());
            assertArrayEquals(new byte[0], response2.getBody());
        });
    }

    @Test
    void upsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();
            final long expire = 1_000;


            // Insert value1
            assertEquals(201, upsert(key, value1, expire).getStatus());

            // Insert value2
            assertEquals(201, upsert(key, value2, expire).getStatus());

            // Check value 2
            final Response response = get(key);
            assertEquals(200, response.getStatus());
            assertArrayEquals(value2, response.getBody());
        });
    }

    @Test
    void deadUpsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value1 = randomValue();
            final byte[] value2 = randomValue();
            final long expire = 1_000;


            // Insert value1
            assertEquals(201, upsert(key, value1, expire).getStatus());

            // Insert value2
            assertEquals(201, upsert(key, value2, expire).getStatus());

            // Check value 2
            Thread.sleep(1_000);
            final Response response = get(key);
            assertEquals(404, response.getStatus());
            assertArrayEquals(new byte[0], response.getBody());
        });
    }

    @Test
    void delete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final String key = randomId();
            final byte[] value = randomValue();
            final long expire = 1_000;

            // Insert
            assertEquals(201, upsert(key, value, expire).getStatus());

            // Delete
            assertEquals(202, delete(key).getStatus());

            // Check
            assertEquals(404, get(key).getStatus());
        });
    }
}
