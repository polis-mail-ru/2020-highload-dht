package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple Http Server Service implementation.
 *
 * @author Kate Moreva
 */

public class MySimpleHttpServer extends HttpServer implements Service {
    private static final String TIMESTAMP = "Timestamp";
    static final String PROXY_HEADER_KEY = "X-Proxy";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);
    static final Duration timeout = Duration.ofSeconds(1);
    private final ExecutorService executorService;
    private final Topology<String> topology;
    private final MyRequestHelper requestHelper;
    private final Replicas quorum;
    private final java.net.http.HttpClient client;

    /**
     * Http Server constructor.
     */
    public MySimpleHttpServer(final int port,
                              final DAO dao,
                              final int numberOfWorkers,
                              final int queueSize,
                              final Topology<String> topology) throws IOException {
        super(getConfig(port, numberOfWorkers));
        this.topology = topology;
        assert numberOfWorkers > 0;
        assert queueSize > 0;
        this.requestHelper = new MyRequestHelper(dao);
        this.executorService = new ThreadPoolExecutor(numberOfWorkers,
                queueSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in {} when processing request", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
        this.quorum = Replicas.quorum(this.topology.size());
        final Executor clientExecutor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors(),
                        new ThreadFactoryBuilder().setNameFormat("client-%d").build());
        this.client = java.net.http.HttpClient.newBuilder()
                .executor(clientExecutor)
                .connectTimeout(timeout)
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
    }

    private static HttpServerConfig getConfig(final int port, final int numberOfWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numberOfWorkers;
        return config;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Method to check whether the server is reachable or not.
     * If the server is available @return {@link Response} {@code 200}.
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        requestHelper.sendLoggedResponse(session, new Response(Response.OK, Response.EMPTY));
    }

    /**
     * Method for working with value in the DAO by the key.
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 201} (new data created).
     * {@code 202} (data deleted).
     * {@code 405} (unexpected method).
     * {@code 500} (internal server error occurred).
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id, final Request request,
                       final HttpSession session, @Param("replicas") final String replicas) {
        try {
            if (id.isBlank()) {
                log.error("Request with empty id on /v0/entity");
                requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }
            executorService.execute(() -> {
                final boolean isProxy = requestHelper.isProxied(request);
                final Replicas replicasFactor = isProxy || replicas == null ? this.quorum : Replicas.parser(replicas);

                if (replicasFactor.getFrom() > this.topology.size()
                        || replicasFactor.getAck() > replicasFactor.getFrom() || replicasFactor.getAck() <= 0) {
                    requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
                defineMethod(request, session, key, replicasFactor, isProxy);
            });
        } catch (RejectedExecutionException e) {
            requestHelper.sendLoggedResponse(session, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    private void defineMethod(final Request request, final HttpSession session, final ByteBuffer key,
                              final Replicas replicasFactor, final boolean isProxy) {
        final Context context = new Context(session, isProxy, request, replicasFactor);
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                executeMethod(context, key, () -> requestHelper.getEntity(key));
                break;
            case Request.METHOD_PUT:
                executeMethod(context, key, () -> requestHelper.putEntity(key, context.getRequest()));
                break;
            case Request.METHOD_DELETE:
                executeMethod(context, key, () -> requestHelper.deleteEntity(key));
                break;
            default:
                log.error("Not allowed method on /v0/entity");
                requestHelper.sendLoggedResponse(session, new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                break;
        }
    }

    void executeMethod(final Context context, final ByteBuffer key, final Action action) {
        if (context.isProxy()) {
            requestHelper.sendLoggedResponse(context.getSession(), action.act());
            return;
        }
        final CompletableFuture<List<ResponseValue>> future = requestHelper.collect(
                replication(action, key, topology, context),
                context.getReplicaFactor().getAck());
        final CompletableFuture<ResponseValue> result = requestHelper.merge(future);
        result.thenAccept(v -> requestHelper.sendLoggedResponse(
                context.getSession(), new Response(v.getStatus(), v.getBody())))
                .exceptionally(e -> {
                    log.error("Error while executing method ", e);
                    return null;
                });
    }

    private List<CompletableFuture<ResponseValue>> replication(final MySimpleHttpServer.Action action,
                                                               final ByteBuffer key,
                                                               final Topology<String> topology,
                                                               final Context context) {
        Set<String> nodes = Collections.emptySet();
        try {
            nodes = topology.primaryFor(key, context.getReplicaFactor(), context.getReplicaFactor().getAck());
        } catch (IllegalArgumentException e) {
            requestHelper.sendLoggedResponse(context.getSession(), new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
        final List<CompletableFuture<ResponseValue>> results = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isMe(node)) {
                getLocalResults(action, results);
            } else {
                final HttpRequest request = requestForReplica(context.getRequest(), key, context, node);
                final CompletableFuture<ResponseValue> result = this.client
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                        .thenApply(r -> new ResponseValue(requestHelper.parseStatusCode(r.statusCode()), r.body(),
                                r.headers().firstValueAsLong(TIMESTAMP).orElse(-1)));
                results.add(result);
            }
        }
        return results;
    }

    private void getLocalResults(final Action action, final List<CompletableFuture<ResponseValue>> results) {
        final CompletableFuture<ResponseValue> future = CompletableFuture.supplyAsync(() -> {
            final Response response = action.act();
            return new ResponseValue(requestHelper.parseStatusCode(response.getStatus()),
                    response.getBody(), requestHelper.getTimestamp(response));
        });
        results.add(future);
    }

    private HttpRequest requestForReplica(final Request request, final ByteBuffer key,
                                          final Context context, final String node) {
        final URI uri = URI.create(node + "/v0/entity?id="
                + StandardCharsets.UTF_8.decode(key.duplicate()).toString()
                + "&replicas=" + context.getReplicaFactor().toString());
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(MySimpleHttpServer.timeout)
                .uri(uri)
                .headers(PROXY_HEADER_KEY, "true");
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return builder.GET().build();
            case Request.METHOD_PUT:
                return builder.PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody())).build();
            case Request.METHOD_DELETE:
                return builder.DELETE().build();
            default:
                throw new UnsupportedOperationException(request.getMethod() + Response.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error can't shutdown execution service");
            Thread.currentThread().interrupt();
        }
    }

    public interface Action {
        Response act();
    }
}
