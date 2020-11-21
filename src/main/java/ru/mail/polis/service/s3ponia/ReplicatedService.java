package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.Proxy;
import ru.mail.polis.util.Utility;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ReplicatedService implements HttpEntityHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReplicatedService.class);
    final AsyncService asyncService;
    final ShardingPolicy<ByteBuffer, String> policy;
    final HttpClient httpClient;

    /**
     * Creates a new {@link ReplicatedService} with given {@link AsyncService} and {@link ShardingPolicy}.
     *
     * @param service {@link HttpEntityHandler} base service for proxy handle
     * @param policy  {@link ShardingPolicy} replica policy
     */
    public ReplicatedService(@NotNull final AsyncService service,
                             @NotNull final ShardingPolicy<ByteBuffer, String> policy) {
        this.asyncService = service;
        this.policy = policy;
        final var executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("client-%d")
                        .build()
        );
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(1))
                .executor(executor)
                .build();
    }

    private static URI entityUri(
            @NotNull final String node,
            @NotNull final String id) {
        try {
            return new URI(node + "/v0/entity?id=" + id);
        } catch (URISyntaxException e) {
            logger.error("Error in uri", e);
            throw new RuntimeException("Error in URI parsing", e);
        }
    }

    private static HttpRequest.Builder request(
            @NotNull final URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(1));
    }

    private static HttpRequest getRequest(
            @NotNull final URI uri) {
        return request(uri)
                .header(Proxy.PROXY_HEADER, "get")
                .GET()
                .build();
    }

    private static HttpRequest putRequest(
            @NotNull final URI uri,
            final long time,
            @NotNull final byte[] body) {
        return request(uri)
                .header(Proxy.PROXY_HEADER, "put")
                .header(Utility.TIME_HEADER, Long.toString(time))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private static HttpRequest deleteRequest(
            @NotNull final URI uri,
            final long time) {
        return request(uri)
                .header(Proxy.PROXY_HEADER, "delete")
                .header(Utility.TIME_HEADER, Long.toString(time))
                .DELETE()
                .build();
    }

    private FutureValues<Value> get(
            @NotNull final String id,
            @NotNull final String... nodes) {
        final var futureValues = Proxy.proxyReplicasAsync(
                httpClient,
                new GetBodyHandler(),
                Arrays.stream(nodes)
                        .filter(n -> !n.equals(policy.homeNode()))
                        .map(n -> getRequest(entityUri(n, id)))
                        .collect(Collectors.toList())
        );
        if (Utility.arrayContains(policy.homeNode(), nodes)) {
            futureValues.add(
                    new FutureValue<>(asyncService
                            .getAsync(Utility.byteBufferFromString(id))
                            .thenApply(Value::fromResponse)
                    )
            );
        }
        return futureValues;
    }

    private FutureValues<Void> put(
            @NotNull final String id,
            final long time,
            @NotNull final byte[] body,
            @NotNull final String... nodes) {
        final var futureValues = Proxy.proxyReplicasAsync(
                httpClient,
                new UpsertBodyHandler(),
                Arrays.stream(nodes)
                        .filter(n -> !n.equals(policy.homeNode()))
                        .map(n -> putRequest(entityUri(n, id), time, body))
                        .collect(Collectors.toList())
        );
        if (Utility.arrayContains(policy.homeNode(), nodes)) {
            futureValues.add(
                    new FutureValue<>(asyncService
                            .putAsync(Utility.byteBufferFromString(id),
                                    ByteBuffer.wrap(body), time)
                            .thenApply(a -> null)
                    )
            );
        }
        return futureValues;
    }

    private FutureValues<Void> delete(
            @NotNull final String id,
            final long time,
            @NotNull final String... nodes) {
        final var futureValues = Proxy.proxyReplicasAsync(
                httpClient,
                new DeleteBodyHandler(),
                Arrays.stream(nodes)
                        .filter(n -> !n.equals(policy.homeNode()))
                        .map(n -> deleteRequest(entityUri(n, id), time))
                        .collect(Collectors.toList())
        );

        if (Utility.arrayContains(policy.homeNode(), nodes)) {
            futureValues.add(
                    new FutureValue<>(asyncService
                            .deleteAsync(Utility.byteBufferFromString(id), time)
                            .thenApply(a -> null)
                    )
            );
        }
        return futureValues;
    }

    @Override
    public void entity(@NotNull final String id,
                       final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        final var proxyHeader = Header.getHeader(Proxy.PROXY_HEADER, request);
        if (proxyHeader != null) {
            asyncService.entity(id, replicas, request, session);
            return;
        }
        
        final var time = System.currentTimeMillis();
        request.addHeader(Utility.TIME_HEADER + ": " + time);

        final ReplicationConfiguration parsedReplica;
        try {
            parsedReplica = ReplicationConfiguration.parseOrDefault(replicas, policy.all().length);
        } catch (IllegalArgumentException e) {
            logger.error("Bad request", e);
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new RuntimeException("Bad request's method", e);
        }

        final var key = Utility.byteBufferFromString(id);
        final var nodes = policy.getNodeReplicas(key, parsedReplica.replicas);

        final var factory = new ResolvedFactory(parsedReplica.acks, request.getMethod());
        final CompletableFuture<Response> resolvedFutureResponse;

        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    resolvedFutureResponse = factory.resolvedFutureReplicaResponses(
                            get(id, nodes)
                    ).resolved();
                    break;
                }
                case Request.METHOD_PUT: {
                    resolvedFutureResponse = factory.resolvedFutureReplicaResponses(
                            put(id, time, request.getBody(), nodes)
                    ).resolved();
                    break;
                }
                case Request.METHOD_DELETE: {
                    resolvedFutureResponse = factory.resolvedFutureReplicaResponses(
                            delete(id, time, nodes)
                    ).resolved();
                    break;
                }
                default: {
                    session.sendError(Response.BAD_REQUEST, "Invalid method");
                    throw new IllegalArgumentException("Invalid method");
                }
            }
        } catch (InvalidRequestMethod e) {
            session.sendError(Response.BAD_REQUEST, "Invalid request");
            throw new IllegalArgumentException("Invalid request", e);
        }

        if (resolvedFutureResponse.whenComplete((r, t) -> {
            try {
                if (t == null) {
                    session.sendResponse(r);
                } else {
                    logger.error("Logic error. resolvedResponse must not complete exceptionally", t);
                    session.sendError(Response.INTERNAL_ERROR, "ResolvedResponse complete exceptionally");
                }
            } catch (IOException e) {
                logger.error("Exception in sending response");
            }
        }).isCancelled()) {
            logger.error("Canceled resolve task");
            session.sendError(Response.INTERNAL_ERROR, "Canceled resolve task");
        }
    }

    @Override
    public void close() throws IOException {
        asyncService.close();
    }
}
