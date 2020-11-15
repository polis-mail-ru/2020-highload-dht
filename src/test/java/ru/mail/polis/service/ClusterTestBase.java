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

import one.nio.http.HttpClient;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import ru.mail.polis.Files;
import ru.mail.polis.TestBase;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Facilities for cluster tests.
 *
 * @author Vadim Tsesko
 */
abstract class ClusterTestBase extends TestBase {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private final Map<String, HttpClient> hostToClient = new HashMap<>();

    private String[] nodes;
    private int[] ports;
    private File[] datas;
    private DAO[] daos;
    private Service[] services;
    private Set<String> endpoints;

    @NotNull
    private static String path(@NotNull final String id) {
        return "/v0/entity?id=" + id;
    }

    @NotNull
    private static String path(
            @NotNull final String id,
            final int ack,
            final int from) {
        return path(id) + "&replicas=" + ack + "/" + from;
    }

    @BeforeEach
    void beforeEach() throws Exception {
        final int size = getClusterSize();

        // Allocate structures
        nodes = new String[size];
        ports = new int[size];
        datas = new File[size];
        daos = new DAO[size];
        services = new Service[size];
        endpoints = new HashSet<>(size);

        // Choose ports and build topology
        for (int i = 0; i < size; i++) {
            int port;
            do {
                port = randomPort();
            } while (endpoints.contains(endpoint(port)));
            ports[i] = port;
            nodes[i] = endpoint(ports[i]);
            endpoints.add(nodes[i]);
        }

        // Initialize and start each node
        for (int i = 0; i < size; i++) {
            datas[i] = Files.createTempDirectory();
            daos[i] = DAOFactory.create(datas[i]);
            createAndStart(i);
        }
    }

    @AfterEach
    void afterEach() throws IOException {
        for (int i = 0; i < getClusterSize(); i++) {
            if (services[i] == null) {
                continue;
            }

            stop(i);
            daos[i].close();
            daos[i] = null;
            Files.recursiveDelete(datas[i]);
            datas[i] = null;
        }
        endpoints.clear();
    }

    abstract int getClusterSize();

    void waitForVersionAdvancement() throws Exception {
        Thread.sleep(10);
    }

    @NotNull
    private HttpClient client(final int node) {
        final String endpoint = nodes[node];
        return hostToClient.computeIfAbsent(
                endpoint,
                key -> new HttpClient(new ConnectionString(key + "?timeout=" + TIMEOUT.dividedBy(2).toMillis())));
    }

    private void resetClient(final int node) {
        final String endpoint = nodes[node];
        final HttpClient client = hostToClient.remove(endpoint);
        if (client != null) {
            client.close();
        }
    }

    @NotNull
    Service service(final int node) {
        return services[node];
    }

    void restartAllNodes() throws Exception {
        for (int i = 0; i < getClusterSize(); i++) {
            if (services[i] != null) {
                stop(i);
            }
            createAndStart(i);
        }
    }

    void stop(final int node) {
        resetClient(node);
        service(node).stop();
        services[node] = null;
    }

    void createAndStart(final int node) throws Exception {
        assert services[node] == null;
        services[node] = ServiceFactory.create(ports[node], daos[node], endpoints);
        services[node].start();

        // Wait for the server to answer
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);
        while (System.currentTimeMillis() < deadline) {
            try {
                if (client(node).get("/v0/status").getStatus() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // We are waiting
            }
        }
        throw new RuntimeException("Can't wait for the service");
    }

    Response get(
            final int node,
            @NotNull final String key) throws Exception {
        return client(node).get(path(key));
    }

    void checkResponse(
            final int expectedStatus,
            final byte[] expectedBody,
            @NotNull final Response response) {
        assertEquals(expectedStatus, response.getStatus());
        assertArrayEquals(expectedBody, response.getBody());
    }

    Response get(
            final int node,
            @NotNull final String key,
            final int ack,
            final int from) throws Exception {
        return client(node).get(path(key, ack, from));
    }

    Response delete(
            final int node,
            @NotNull final String key) throws Exception {
        return client(node).delete(path(key));
    }

    Response delete(
            final int node,
            @NotNull final String key,
            final int ack,
            final int from) throws Exception {
        return client(node).delete(path(key, ack, from));
    }

    Response upsert(
            final int node,
            @NotNull final String key,
            @NotNull final byte[] data) throws Exception {
        return client(node).put(path(key), data);
    }

    Response upsert(
            final int node,
            @NotNull final String key,
            @NotNull final byte[] data,
            final int ack,
            final int from) throws Exception {
        return client(node).put(path(key, ack, from), data);
    }
}
