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

import com.google.common.collect.Iterators;
import one.nio.http.HttpClient;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.TestBase;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Facilities for cluster tests.
 *
 * @author Vadim Tsesko
 */
abstract class ClusterTestBase extends TestBase {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private final Map<String, HttpClient> hostToClient = new HashMap<>();
    Set<String> endpoints;

    @NotNull
    private HttpClient client(final int node) {
        final String endpoint = Iterators.get(endpoints.iterator(), node);
        return hostToClient.computeIfAbsent(
                endpoint,
                key -> new HttpClient(new ConnectionString(key + "?timeout=" + TIMEOUT.dividedBy(2).toMillis())));
    }

    private void resetClient(final int node) {
        final String endpoint = Iterators.get(endpoints.iterator(), node);
        final HttpClient client = hostToClient.remove(endpoint);
        if (client != null) {
            client.close();
        }
    }

    void stop(
            final int node,
            @NotNull final Service service) {
        resetClient(node);
        service.stop();
    }

    void start(
            final int node,
            @NotNull final Service service) {
        service.start();

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

    Response get(
            final int node,
            @NotNull final String key) throws Exception {
        return client(node).get(path(key));
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
