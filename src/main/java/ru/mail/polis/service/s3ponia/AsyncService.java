package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.s3ponia.Proxy;
import ru.mail.polis.s3ponia.RunnableWithException;
import ru.mail.polis.s3ponia.SupplierWithException;
import ru.mail.polis.s3ponia.Utility;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static ru.mail.polis.s3ponia.Utility.badRequestResponse;

public final class AsyncService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    private final DAO dao;
    private final ExecutorService es;
    private final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;
    private final Map<Integer, Response> mapResponseOnSuccess =
            Map.of(Request.METHOD_DELETE, new Response(Response.ACCEPTED, Response.EMPTY),
                    Request.METHOD_PUT, new Response(Response.CREATED, Response.EMPTY),
                    Request.METHOD_GET, Response.ok(Response.EMPTY));

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
        BasicConfigurator.configure();
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

    /**
     * Entity requests handler.
     *
     * @param id       dao's key
     * @param replicas replica configuration param
     * @param request  handling request
     * @param session  current session
     * @throws IOException rethrow session exception
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (!Utility.validateId(id)) {
            badRequestResponse(session,
                    String.format("Empty id in request %s with method %s", request.getURI(), request.getMethodName()),
                    logger);
            throw new RuntimeException("Empty id");
        }

        final var proxyHeader = Header.getHeader(Proxy.PROXY_HEADER, request);
        final ByteBuffer value;
        if (request.getBody() == null) {
            value = ByteBuffer.allocate(0);
        } else {
            value = ByteBuffer.wrap(request.getBody());
        }

        final var header = Header.getHeader(Utility.TIME_HEADER, request);

        if (header == null && request.getMethod() != Request.METHOD_GET && proxyHeader != null) {
            badRequestResponse(session, "Mismatch headers", logger);
            throw new RuntimeException("Mismatch headers");
        }

        long time = 0;
        if (header != null) {
            try {
                time = Long.parseLong(header.value);
            } catch (NumberFormatException e) {
                badRequestResponse(
                        session, String.format("FormatException in time header %s", header.value), e, logger);
                throw e;
            }
        }
        final var key = Utility.byteBufferFromString(id);
        final Supplier<CompletableFuture<Response>> proxyHandler;
        try {
            proxyHandler = noReplicaHandler(key, value, time, request.getMethod());
        } catch (IllegalArgumentException e) {
            badRequestResponse(
                    session, String.format("Bad request's method %s", request.getMethodName()), e, logger);
            throw new RuntimeException("Bad request's method", e);
        }
        if (proxyHeader != null) {
            handleNoReplicaOperation(session, proxyHandler);
            return;
        }
        final ReplicationConfiguration parsedReplica;
        try {
            parsedReplica = ReplicationConfiguration.parseOrDefault(replicas, policy.all().length);
        } catch (IllegalArgumentException e) {
            badRequestResponse(session, "Bad replica param", e, logger);
            return;
        }
        final var currTime = System.currentTimeMillis();

        request.addHeader(Utility.TIME_HEADER + ": " + currTime);
        final var nodes = policy.getNodeReplicas(key, parsedReplica.replicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);

        final var replicaResponses =
                Proxy.proxyReplicas(request, urlToClient::get, policy.homeNode(), nodes);
        final RunnableWithException daoOp = getMapDaoOp(value, key, currTime).get(request.getMethod());
        final Response responseSucc = mapResponseOnSuccess.get(request.getMethod());
        if (request.getMethod() == Request.METHOD_GET) {
            try {
                resolveGetProxyResult(session, parsedReplica, homeInReplicas, replicaResponses, () -> dao.getRaw(key));
            } catch (ReplicaException e) {
                logger.error("Not enough replicas response");
                session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
            }
        } else {
            final int counter = replicaResponses.size() + daoOp.returnPlusCountIfNoException(homeInReplicas);
            try {
                resolvePutDeleteProxyResult(session, parsedReplica, responseSucc, counter);
            } catch (ReplicaException e) {
                logger.error("Not enough replica");
                session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
            }
        }
    }

    private void handleNoReplicaOperation(@NotNull final HttpSession session,
                                          @NotNull final Supplier<CompletableFuture<Response>> proxyHandler)
            throws IOException {
        try {
            Proxy.proxyHandle(session, proxyHandler);
        } catch (CancellationException e) {
            logger.error("Canceled task", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        } catch (RejectedExecutionException e) {
            logger.error("Rejected task", e);
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    @NotNull
    private Map<Integer, RunnableWithException> getMapDaoOp(ByteBuffer value, ByteBuffer key, long currTime) {
        return Map.of(Request.METHOD_DELETE, () -> dao.removeWithTimeStamp(key, currTime),
                Request.METHOD_PUT, () -> dao.upsertWithTimeStamp(key, value, currTime),
                Request.METHOD_GET, () -> dao.getRaw(key));
    }

    private static void resolvePutDeleteProxyResult(@NotNull final HttpSession session,
                                                    @NotNull final ReplicationConfiguration parsedReplica,
                                                    @NotNull final Response succResp,
                                                    final int counter) throws IOException, ReplicaException {
        if (counter < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas");
        } else {
            session.sendResponse(succResp);
        }
    }

    private static void resolveGetProxyResult(@NotNull final HttpSession session,
                                              @NotNull final ReplicationConfiguration parsedReplica,
                                              final boolean homeInReplicas,
                                              @NotNull final List<Response> replicaResponses,
                                              @NotNull final SupplierWithException<Value> daoOp) throws IOException,
            ReplicaException {
        final var values = Utility.getValuesFromResponses(parsedReplica, replicaResponses);
        if (homeInReplicas) {
            try {
                values.add(daoOp.get());
            } catch (IOException ignored) {
                logger.error("Error in getting from dao");
            }
        }
        if (values.size() < parsedReplica.acks) {
            throw new ReplicaException("Not enough replicas");
        }

        values.sort(Value.valueResponseComparator());

        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } else {
            session.sendResponse(Response.ok(Utility.fromByteBuffer(bestVal.getValue())));
        }
    }

    private Supplier<CompletableFuture<Response>> noReplicaHandler(@NotNull final ByteBuffer key,
                                                                   @NotNull final ByteBuffer value,
                                                                   final long time,
                                                                   final int method) {
        final Map<Integer, Supplier<CompletableFuture<Response>>> map =
                Map.of(Request.METHOD_GET, handleNoReplicaGet(key),
                        Request.METHOD_PUT, handleNoReplicaPut(key, value, time),
                        Request.METHOD_DELETE, handleNoReplicaDelete(key, time));
        return map.get(method);
    }

    private Supplier<CompletableFuture<Response>> handleNoReplicaDelete(@NotNull final ByteBuffer key,
                                                                        final long time) {
        return () -> CompletableFuture.<Void>supplyAsync(() -> {
            try {
                dao.removeWithTimeStamp(key, time);
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao.removeWithTimeStamp", e);
            }
        }, es).thenApply((v) -> new Response(Response.CREATED, Response.EMPTY))
                .exceptionally((t) -> {
                    logger.error("Error in upserting in dao", t);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                });
    }

    private Supplier<CompletableFuture<Response>> handleNoReplicaPut(@NotNull final ByteBuffer key,
                                                                     @NotNull final ByteBuffer value,
                                                                     final long time) {
        return () -> CompletableFuture.<Void>supplyAsync(() -> {
            try {
                dao.upsertWithTimeStamp(key, value, time);
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao.upsertWithTimeStamp", e);
            }
        }, es).thenApply((v) -> new Response(Response.CREATED, Response.EMPTY))
                .exceptionally((t) -> {
                    logger.error("Error in upserting in dao", t);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                });
    }

    private Supplier<CompletableFuture<Response>> handleNoReplicaGet(@NotNull final ByteBuffer key) {
        return () -> CompletableFuture.supplyAsync(() -> {
            try {
                return dao.getRaw(key);
            } catch (IOException e) {
                throw new RuntimeException("IOException in dao.getRAW", e);
            }
        }, es).thenApply((v) -> {
            final var resp = Response.ok(Utility.fromByteBuffer(v.getValue()));
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        });
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
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
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
