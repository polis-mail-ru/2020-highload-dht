package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.Socket;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.mail.polis.service.kate.moreva.MyRequestHelper.PROXY_HEADER;

/**
 * Simple Http Server Service implementation.
 *
 * @author Kate Moreva
 */

public class MySimpleHttpServer extends HttpServer implements Service {
    private static final String TIMESTAMP = "Timestamp";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);
    private final ExecutorService executorService;
    private final Executor clientExecutor;
    private final Topology<String> topology;
    private final MyRequestHelper requestHelper;
    private final Replicas quorum;
    private final HttpClient client;


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
        this.clientExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("client-%d").build());
        this.client = HttpClient.newBuilder()
                .executor(clientExecutor)
                .connectTimeout(Duration.ofSeconds(1))
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
        if (id.isBlank()) {
            log.error("Request with empty id on /v0/entity");
            requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        try {
            executorService.execute(() -> parseRequest(id, request, session, replicas));
        } catch (RejectedExecutionException e) {
            requestHelper.sendLoggedResponse(session, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    /**
     * Method for returning set of values in the requested range.
     */
    @Path("/v0/entities")
    public void entities(@NotNull final HttpSession session, final Request request,
                         @Param("start") final String start,
                         @Param("end") final String end, @Param("replicas") final String replicas) {
        if (start == null || start.isEmpty() || (end != null && end.isEmpty())) {
            log.error("Request with incorrect parameters start {}, end {} on /v0/entities", start, end);
            requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        final ByteBuffer startRange = ByteBuffer.wrap(start.getBytes(Charsets.UTF_8));
        final ByteBuffer endRange = end == null ? null : ByteBuffer.wrap(end.getBytes(Charsets.UTF_8));
        parseRangeRequest(startRange, endRange, request, session, replicas);
    }
    private void parseRangeRequest(final ByteBuffer start, final ByteBuffer end, final Request request,
                               final HttpSession session, final String replicas) {
        final boolean isProxy = requestHelper.isProxied(request);
        try {
            final Replicas replicasFactor = isProxy
                    || replicas == null ? this.quorum : Replicas.parser(replicas);
            if (replicasFactor.getFrom() > this.topology.size()
                    || replicasFactor.getAck() > replicasFactor.getFrom() || replicasFactor.getAck() <= 0) {
                requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }
            final Context context = new Context(session, isProxy, request, replicasFactor);
            requestHelper.workRangeRequest(context.getSession(), start, end, clientExecutor);
        } catch (IllegalArgumentException e) {
            requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
    }

    private void parseRequest(final String id, final Request request,
                              final HttpSession session, final String replicas) {
        final boolean isProxy = requestHelper.isProxied(request);
        try {
            final Replicas replicasFactor = isProxy
                    || replicas == null ? this.quorum : Replicas.parser(replicas);
            if (replicasFactor.getFrom() > this.topology.size()
                    || replicasFactor.getAck() > replicasFactor.getFrom() || replicasFactor.getAck() <= 0) {
                requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
            final Context context = new Context(session, isProxy, request, replicasFactor);
            defineMethod(key, context);
        } catch (IllegalArgumentException e) {
            requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
    }

    private void defineMethod(final ByteBuffer key, final Context context) {
        CompletableFuture.runAsync(() -> {
            switch (context.getRequest().getMethod()) {
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
                    requestHelper.sendLoggedResponse(context.getSession(),
                            new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    break;
            }
        }, clientExecutor).exceptionally(e -> {
            log.error("Error while executing method ", e);
            return null;
        });
    }

    private void executeMethod(final Context context, final ByteBuffer key, final Action action) {
        if (context.isProxy()) {
            requestHelper.sendLoggedResponse(context.getSession(), action.act());
            return;
        }
        try {
            final CompletableFuture<List<ResponseValue>> future = requestHelper.collect(
                    replication(action, key, topology, context),
                    context.getReplicaFactor().getAck(), clientExecutor);
            final CompletableFuture<ResponseValue> result = requestHelper.merge(future);
            result.thenAccept(v -> requestHelper.sendLoggedResponse(
                    context.getSession(), new Response(v.getStatus(), v.getBody())))
                    .exceptionally(e -> {
                        log.error("Error while executing method ", e);
                        requestHelper.sendLoggedResponse(context.getSession(),
                                new Response(requestHelper.parseStatusCode(504), Response.EMPTY));
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            requestHelper.sendLoggedResponse(context.getSession(),
                    new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private List<CompletableFuture<ResponseValue>> replication(final Action action, final ByteBuffer key,
                                                               final Topology<String> topology, final Context context)
            throws IllegalArgumentException {
        final List<CompletableFuture<ResponseValue>> results = new ArrayList<>();
        final Set<String> nodes = topology.primaryFor(key, context.getReplicaFactor(),
                context.getReplicaFactor().getAck());
        for (final String node : nodes) {
            if (topology.isMe(node)) {
                results.add(getLocalResults(action));
            } else {
                final URI uri = URI.create(node + "/v0/entity?id="
                        + StandardCharsets.UTF_8.decode(key.duplicate()).toString());
                final HttpRequest request = requestHelper.requestForReplica(context.getRequest(), uri);
                final CompletableFuture<ResponseValue> result = this.client
                        .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                        .thenApply(r -> new ResponseValue(requestHelper.parseStatusCode(r.statusCode()), r.body(),
                                r.headers().firstValueAsLong(TIMESTAMP).orElse(-1)));
                results.add(result);
            }
        }
        return results;
    }

    private CompletableFuture<ResponseValue> getLocalResults(final Action action) throws IllegalArgumentException {
        return CompletableFuture.supplyAsync(() -> {
            final Response response = action.act();
            return new ResponseValue(requestHelper.parseStatusCode(response.getStatus()),
                    response.getBody(), requestHelper.getTimestamp(response));
        }, clientExecutor);
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

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StreamingSession(socket, this);
    }

    public interface Action {
        Response act();
    }
}
