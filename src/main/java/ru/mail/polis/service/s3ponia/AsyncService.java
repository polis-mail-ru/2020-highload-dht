package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.s3ponia.Utility;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AsyncService extends HttpServer implements Service {
    public static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    public static final byte[] EMPTY = Response.EMPTY;
    public static final List<ReplicationConfiguration> DEFAULT_CONFIGURATIONS = Arrays.asList(
            new ReplicationConfiguration(1, 1),
            new ReplicationConfiguration(2, 2),
            new ReplicationConfiguration(2, 3),
            new ReplicationConfiguration(3, 4),
            new ReplicationConfiguration(3, 5)
    );
    public final DAO dao;
    public final ExecutorService es;
    public final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;

    /**
     * AsyncService's constructor.
     *
     * @param port      port
     * @param dao       dao
     * @param workers   workers count
     * @param queueSize queue's size
     * @param policy    policy
     * @throws IOException rethrow ioexception
     */
    public AsyncService(final int port, @NotNull final DAO dao,
                        final int workers, final int queueSize,
                        @NotNull final ShardingPolicy<ByteBuffer, String> policy) throws IOException {
        super(Utility.configFrom(port));
        assert 0 < workers;
        assert 0 < queueSize;
        this.policy = policy;
        this.urlToClient = Utility.urltoClientFromSet(this.policy.homeNode(), this.policy.all());
        this.dao = dao;
        this.es = new ThreadPoolExecutor(
                workers,
                workers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Error in {} when processing request",
                                t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @NotNull
    private List<Response> proxyReplicas(@NotNull final Request request,
                                         @NotNull final String... nodes) {
        final List<Response> futureResponses = new ArrayList<>(nodes.length);

        for (final var node : nodes) {

            if (!node.equals(policy.homeNode())) {
                final var response = proxy(node, request);
                if (response == null) {
                    continue;
                }
                if (response.getStatus() != 202 /* ACCEPTED */
                        && response.getStatus() != 201 /* CREATED */
                        && response.getStatus() != 200 /* OK */
                        && response.getStatus() != 404 /* NOT FOUND */) {
                    continue;
                }
                futureResponses.add(response);
            }
        }
        return futureResponses;
    }

    private static void badRequestResponse(@NotNull Logger logger,
                                           @NotNull final HttpSession session,
                                           @NotNull final String logString) throws IOException {
        logger.error(logString);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }

    private static void badRequestResponse(@NotNull Logger logger,
                                           @NotNull final HttpSession session,
                                           @NotNull final String logString,
                                           @NotNull final Throwable e) throws IOException {
        logger.error(logString, e);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }

    private static void sendResponse(@NotNull final HttpSession session,
                                     @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            logger.error("IOException in sending response", e);
        }
    }

    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (!Utility.validateId(id)) {
            badRequestResponse(logger, session,
                    String.format("Empty id in request %s with method %s", request.getURI(), request.getMethodName()));
            return;
        }

        final String replicaConf;
        if (replicas == null) {
            replicaConf = DEFAULT_CONFIGURATIONS.get(policy.all().length - 1).toString();
        } else {
            replicaConf = replicas;
        }

        final var proxyHeader = Header.getHeader(Utility.PROXY_HEADER, request);
        final var header = Header.getHeader(Utility.TIME_HEADER, request);
        final ReplicationConfiguration parsedReplica = ReplicationConfiguration.parse(replicaConf);
        final var key = Utility.byteBufferFromString(id);
        final ByteBuffer value;
        if (request.getBody() == null) {
            value = ByteBuffer.allocate(0);
        } else {
            value = ByteBuffer.wrap(request.getBody());
        }
        final var currTime = System.currentTimeMillis();
        final long time;

        if (parsedReplica == null || parsedReplica.acks == 0 || parsedReplica.acks > parsedReplica.replicas) {
            badRequestResponse(logger, session, String.format("Bad replicas param %s", parsedReplica));
            return;
        }

        if (header != null) {
            try {
                time = Long.parseLong(header.value);
            } catch (NumberFormatException e) {
                badRequestResponse(logger,
                        session, String.format("FormatException in time header %s", header.value), e);
                return;
            }
        } else {
            time = 0;
            if (request.getMethod() != Request.METHOD_GET && proxyHeader != null) {
                badRequestResponse(logger, session, "Mismatch headers");
                return;
            }
        }

        final Supplier<CompletableFuture<Response>> proxyHandler;
        try {
            proxyHandler = proxyHandler(key, value, time, request.getMethod());
        } catch (IllegalArgumentException e) {
            badRequestResponse(logger,
                    session, String.format("Bad request's method %s", request.getMethodName()), e);
            return;
        }

        if (proxyHeader != null) {
            try {
                if (proxyHandler.get()
                        .whenComplete((r, t) -> proxyWhenCompleteHandler(session, r, t))
                        .isCancelled()) {
                    logger.error("Canceled task");
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    return;
                }
            } catch (RejectedExecutionException e) {
                logger.error("Rejected task", e);
                session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                return;
            }
            return;
        }

        request.addHeader(Utility.TIME_HEADER + ": " + currTime);
        final var nodes = policy.getNodeReplicas(key, parsedReplica.replicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);

        final var replicaResponses = proxyReplicas(request, nodes);
        switch (request.getMethod()) {
            case Request.METHOD_GET: {
                final var values = Utility.getValuesFromResponses(parsedReplica, replicaResponses);
                if (homeInReplicas) {
                    try {
                        values.add(dao.getRaw(key));
                    } catch (IOException e) {
                        logger.error("Error in getting from local dao");
                    }
                }
                if (values.size() < parsedReplica.acks) {
                    logger.error("Not enough replicas response");
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                    return;
                }

                values.sort(Value.valueResponseComparator());

                final var bestVal = values.get(0);
                if (bestVal.isDead()) {
                    session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
                } else {
                    session.sendResponse(Response.ok(Utility.fromByteBuffer(bestVal.getValue())));
                }
                break;
            }
            case Request.METHOD_PUT: {
                int counter = replicaResponses.size();
                if (homeInReplicas) {
                    try {
                        dao.upsertWithTimeStamp(key, value, currTime);
                        counter = replicaResponses.size() + 1;
                    } catch (IOException e) {
                        logger.error("Error in upserting in local dao", e);
                    }
                }

                if (counter < parsedReplica.acks) {
                    logger.error("Not enough replicas response");
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                    return;
                }

                session.sendResponse(new Response(Response.CREATED, EMPTY));
                break;
            }
            case Request.METHOD_DELETE: {
                int counter = replicaResponses.size();
                if (homeInReplicas) {
                    try {
                        dao.removeWithTimeStamp(key, currTime);
                        counter = replicaResponses.size() + 1;
                    } catch (IOException e) {
                        logger.error("Error in upserting in local dao", e);
                    }
                }

                if (counter > parsedReplica.replicas) {
                    logger.error("More responses than needed");
                    session.sendResponse(new Response(Response.INTERNAL_ERROR));
                    return;
                }

                if (counter < parsedReplica.acks) {
                    logger.error("Not enough replicas response");
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                    return;
                }

                session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                break;
            }
            default: {
                logger.error("Logic error");
                session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
            }
        }

    }

    private void proxyWhenCompleteHandler(@NotNull HttpSession session, Response r, Throwable t) {
        if (t == null) {
            sendResponse(session, r);
        } else {
            logger.error("Logic error. t must be null", t);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }

    public Supplier<CompletableFuture<Response>> proxyHandler(@NotNull final ByteBuffer key,
                                                              @NotNull final ByteBuffer value,
                                                              final long time,
                                                              final int method) {
        switch (method) {
            case Request.METHOD_GET: {
                return proxyHandlerGET(key);
            }
            case Request.METHOD_PUT: {
                return proxyHandlerPUT(key, value, time);
            }
            case Request.METHOD_DELETE: {
                return proxyHandlerDELETE(key, time);
            }
            default: {
                throw new IllegalArgumentException("Unhandled method");
            }
        }
    }

    public Supplier<CompletableFuture<Response>> proxyHandlerDELETE(@NotNull final ByteBuffer key,
                                                                    final long time) {
        return () -> CompletableFuture.<Void>supplyAsync(() -> {
            try {
                dao.removeWithTimeStamp(key, time);
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao");
            }
        }, es).thenApply((v) -> new Response(Response.CREATED, Response.EMPTY))
                .exceptionally((t) -> {
                    logger.error("Error in upserting in dao", t);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                });
    }

    public Supplier<CompletableFuture<Response>> proxyHandlerPUT(@NotNull final ByteBuffer key,
                                                                 @NotNull final ByteBuffer value,
                                                                 final long time) {
        return () -> CompletableFuture.<Void>supplyAsync(() -> {
            try {
                dao.upsertWithTimeStamp(key, value, time);
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao");
            }
        }, es).thenApply((v) -> new Response(Response.CREATED, Response.EMPTY))
                .exceptionally((t) -> {
                    logger.error("Error in upserting in dao", t);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                });
    }

    public Supplier<CompletableFuture<Response>> proxyHandlerGET(@NotNull final ByteBuffer key) {
        return () -> CompletableFuture.supplyAsync(() -> {
            try {
                return dao.getRaw(key);
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao");
            }
        }, es).thenApply((v) -> {
            final var resp = Response.ok(Utility.fromByteBuffer(v.getValue()));
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        });
    }

    public Response proxy(
            @NotNull final String node,
            @NotNull final Request request) {
        try {
            request.addHeader(Utility.PROXY_HEADER + ":" + node);
            return urlToClient.get(node).invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            return null;
        }
    }


    /**
     * Handling status request.
     *
     * @param session current Session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) throws IOException {
        session.sendResponse(Response.ok("OK"));
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        this.es.shutdown();
        try {
            this.es.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown executor", e);
            Thread.currentThread().interrupt();
        }

        for (final HttpClient client : this.urlToClient.values()) {
            client.clear();
        }
    }
}
