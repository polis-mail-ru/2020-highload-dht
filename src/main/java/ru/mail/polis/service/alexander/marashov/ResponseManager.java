package ru.mail.polis.service.alexander.marashov;

import one.nio.http.Request;
import one.nio.http.Response;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.alexander.marashov.Value;
import ru.mail.polis.service.alexander.marashov.analyzers.FutureAnalyzer;
import ru.mail.polis.service.alexander.marashov.analyzers.ValuesAnalyzer;
import ru.mail.polis.service.alexander.marashov.bodyHandlers.BodyHandlerGet;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static ru.mail.polis.service.alexander.marashov.ServiceImpl.PROXY_HEADER;
import static ru.mail.polis.service.alexander.marashov.ServiceImpl.TIMESTAMP_HEADER_NAME;
import static ru.mail.polis.service.alexander.marashov.ServiceImpl.log;

public class ResponseManager {

    private final Topology<String> topology;
    private final java.net.http.HttpClient httpClient;
    private final DaoManager daoManager;
    private final ExecutorService proxyExecutor;

    private final Duration getTimeout;
    private final Duration putTimeout;
    private final Duration deleteTimeout;

    /**
     * Object for managing responses.
     * @param dao - DAO instance.
     * @param topology - object with cluster info.
     * @param proxyTimeoutValue - proxy timeout value in milliseconds.
     * @param proxyExecutor - executorService instance where proxy requests should be executed.
     */
    public ResponseManager(
            final DAO dao,
            final ExecutorService daoExecutor,
            final ExecutorService proxyExecutor,
            final Topology<String> topology,
            final int proxyTimeoutValue,
            final int getTimeoutValue,
            final int putTimeoutValue,
            final int deleteTimeoutValue
    ) {
        this.daoManager = new DaoManager(dao, daoExecutor);
        this.topology = topology;
        this.proxyExecutor = proxyExecutor;
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .executor(proxyExecutor)
                .connectTimeout(Duration.ofMillis(proxyTimeoutValue))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();

        this.getTimeout = Duration.ofMillis(getTimeoutValue);
        this.putTimeout = Duration.ofMillis(putTimeoutValue);
        this.deleteTimeout = Duration.ofMillis(deleteTimeoutValue);
    }

    /**
     * Executes a get request to the DAO.
     * @param validParams - validated parameters object.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public CompletableFuture<Response> get(final ValidatedParameters validParams, final Request request) {

        final String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            return daoManager.rowGet(validParams.key).thenApplyAsync(
                    value -> {
                        if (value == null) {
                            log.debug("Local get: Value not found");
                            return new Response(Response.NOT_FOUND, Response.EMPTY);
                        } else if (value.isTombstone()) {
                            log.debug("Local get: value is tombstone");
                            final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                            response.addHeader(TIMESTAMP_HEADER_NAME + ": " + value.getTimestamp());
                            return response;
                        } else {
                            log.debug("Local get: value is present");
                            final Response response = new Response(Response.OK, value.getData().array());
                            response.addHeader(TIMESTAMP_HEADER_NAME + ": " + value.getTimestamp());
                            return response;
                        }
                    },
                    proxyExecutor
            );
        }

        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);

        final List<CompletableFuture<Value>> list = new ArrayList<>(primaries.length);
        for (final String node : primaries) {
            if (topology.isLocal(node)) {
                list.add(daoManager.rowGet(validParams.key));
            } else {
                final HttpRequest httpRequest = requestForReplicaBuilder(node, validParams.rowKey, getTimeout)
                        .GET()
                        .header(PROXY_HEADER, "true")
                        .build();
                final CompletableFuture<Value> future = httpClient.sendAsync(httpRequest, BodyHandlerGet.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, proxyExecutor);
                list.add(future);
            }
        }
        return FutureAnalyzer.atLeastAsync(list, validParams.ack)
                .thenApplyAsync(responses -> ValuesAnalyzer.analyze(responses, validParams.ack), proxyExecutor);
    }

    private static HttpRequest.Builder requestForReplicaBuilder(
            final String node,
            final String id,
            final Duration timeout
    ) {
        final String uri = node + "/v0/entity?id=" + id;
        log.debug("Proxy to {}", uri);
        try {
            return HttpRequest.newBuilder()
                    .timeout(timeout)
                    .uri(new URI(uri));
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URI: " + uri, e);
        }

    }

    /**
     * Executes a put request to the DAO.
     * @param validParams - validated parameters object.
     * @param value - byte buffer with value to put.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public CompletableFuture<Response> put(
            final ValidatedParameters validParams,
            final ByteBuffer value,
            final Request request
    ) {
        final String proxyHeader = request.getHeader(PROXY_HEADER);
        log.debug(Arrays.toString(request.getHeaders()));

        if (proxyHeader != null) {
            ServiceImpl.log.debug("PUT: proxy for me!");
            return daoManager.put(validParams.key, value);
        }
        ServiceImpl.log.debug("PUT: it's not proxy");
        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);

        return executeTasksAsync(
                primaries,
                daoManager.put(validParams.key, value),
                (final String primary) -> {
                    final byte[] array;
                    if (value.hasRemaining()) {
                        array = new byte[value.remaining()];
                        value.get(array);
                    } else {
                        array = new byte[0];
                    }
                    final HttpRequest httpRequest =
                            requestForReplicaBuilder(primary, validParams.rowKey, putTimeout)
                                    .PUT(HttpRequest.BodyPublishers.ofByteArray(array))
                                    .header(PROXY_HEADER, "true")
                                    .build();
                    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())
                            .thenApplyAsync(voidHttpResponse -> new Response(Response.CREATED, Response.EMPTY));
                },
                responses -> responses.size() >= validParams.ack
                        ? new Response(Response.CREATED, Response.EMPTY)
                        : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY),
                validParams.ack
        );
    }

    /**
     * Executes a delete request to the DAO.
     * @param validParams - validated parameters object.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public CompletableFuture<Response> delete(final ValidatedParameters validParams, final Request request) {
        final String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            return daoManager.delete(validParams.key);
        }

        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);

        return executeTasksAsync(
                primaries,
                daoManager.delete(validParams.key),
                (final String primary) -> {
                    final HttpRequest httpRequest =
                            requestForReplicaBuilder(primary, validParams.rowKey, deleteTimeout)
                                    .DELETE()
                                    .header(PROXY_HEADER, "true")
                                    .build();
                    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())
                            .thenApplyAsync(voidHttpResponse -> new Response(Response.ACCEPTED, Response.EMPTY));
                },
                responses -> responses.size() >= validParams.ack
                        ? new Response(Response.ACCEPTED, Response.EMPTY)
                        : new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY),
                validParams.ack

        );
    }

    private CompletableFuture<Response> executeTasksAsync(
            final String[] nodes,
            final CompletableFuture<Response> localTask,
            final Function<String, CompletableFuture<Response>> proxyTaskFunction,
            final Function<Collection<Response>, Response> analyzer,
            final int ack
    ) {
        final List<CompletableFuture<Response>> list = new ArrayList<>(nodes.length);
        for (final String node : nodes) {
            if (topology.isLocal(node)) {
                list.add(localTask);
            } else {
                list.add(proxyTaskFunction.apply(node));
            }
        }
        return FutureAnalyzer.atLeastAsync(list, ack)
                .thenApplyAsync(analyzer, proxyExecutor);
    }

    /**
     * Closes the daoManager.
     */
    public void clear() {
        daoManager.close();
    }
}
