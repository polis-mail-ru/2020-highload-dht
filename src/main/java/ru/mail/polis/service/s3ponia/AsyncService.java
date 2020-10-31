package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Table;
import ru.mail.polis.s3ponia.AsyncServiceUtility;
import ru.mail.polis.s3ponia.Utility;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class AsyncService extends HttpServer implements Service {
    public static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    public static final byte[] EMPTY = Response.EMPTY;
    public static final List<Utility.ReplicationConfiguration> DEFAULT_CONFIGURATIONS = Arrays.asList(
            new Utility.ReplicationConfiguration(1, 1),
            new Utility.ReplicationConfiguration(2, 2),
            new Utility.ReplicationConfiguration(2, 3),
            new Utility.ReplicationConfiguration(3, 4),
            new Utility.ReplicationConfiguration(3, 5)
    );
    public final DAO dao;
    public final ExecutorService es;
    public final ShardingPolicy<ByteBuffer, String> policy;
    public final Map<String, HttpClient> urlToClient;
    public final java.net.http.HttpClient httpClient;

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
        final var executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("client-%d")
                        .build()
        );
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(1))
                .executor(executor)
                .build();
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
     * Handling status request.
     *
     * @param session current Session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) throws IOException {
        try {
            this.es.execute(() -> {
                AsyncServiceUtility.handleStatusError(session);
            });
        } catch (RejectedExecutionException e) {
            logger.error("Internal error in status handling", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }

    private CompletableFuture<Table.Value> futureGet(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.getRaw(key);
            } catch (IOException e) {
                logger.error("IOException in getRAW", e);
                throw new RuntimeException("IOException in getRAW", e);
            }
        }, this.es);
    }

    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id,
                    @Param(value = "replicas") final String replicas,
                    @NotNull final Request request,
                    @NotNull final HttpSession session) throws IOException {
        if (Utility.validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }

        final var key = Utility.byteBufferFromString(id);
        if (request.getHeader(Utility.PROXY_HEADER) != null) {
            if (futureGet(key).whenCompleteAsync((val, t) -> {
                try {
                    if (t == null) {
                        final var resp = Response.ok(Utility.fromByteBuffer(val.getValue()));
                        resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + val.getDeadFlagTimeStamp());
                        session.sendResponse(resp);
                    } else {
                        logger.error("Error in dao.getRAW");
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    }
                } catch (IOException e) {
                    logger.error("Error in sending getRAW");
                }
            }).isCancelled()) {
                logger.error("Canceled task");
            }
            return;
        }
        final Utility.ReplicationConfiguration parsed =
                AsyncServiceUtility.getReplicationConfiguration(replicas, session, this);
        if (parsed == null) return;

        final var nodeReplicas = policy.getNodeReplicas(key, parsed.from);
        final var futureResponses =
                AsyncServiceUtility.getGetFutures(id, parsed, this, nodeReplicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodeReplicas);

        if (homeInReplicas) {
            futureResponses.add(futureGet(key));
        }

        if (Utility.atLeast(parsed.ack, futureResponses).thenApplyAsync(c ->
                c.stream().min(Utility.valueResponseComparator()))
                .whenCompleteAsync((v, t) -> {
                    try {
                        if (t != null) {
                            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                            return;
                        }

                        if (v.isEmpty() || v.get().isDead()) {
                            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
                            return;
                        }

                        session.sendResponse(Response.ok(Utility.fromByteBuffer(v.get().getValue())));
                    } catch (IOException e) {
                        logger.error("Error sending response");
                    }
                }).isCancelled()) {
            logger.error("Cancelled get");
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id      key
     * @param request value for putting in database
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(value = "id", required = true) final String id,
                    @Param(value = "replicas") final String replicas,
                    @NotNull final Request request,
                    @NotNull final HttpSession session) throws IOException {
        if (Utility.validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }

        try {
            AsyncServiceUtility.putImpl(id, replicas, request, session, this);
        } catch (RejectedExecutionException e) {
            logger.error("Error in execute", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }

    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (Utility.validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }

        final var key = Utility.byteBufferFromString(id);
        final var header = Utility.Header.getHeader(Utility.TIME_HEADER, request);
        if (header != null) {
            if (AsyncServiceUtility.deleteWithTimeStampAsync(key, Long.parseLong(header.value), this)
            .whenCompleteAsync((v, t) -> {
                try {
                    if (t == null) {
                        session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                    } else {
                        logger.error("Error in removing from DAO");
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
                    }
                } catch (IOException e) {
                    logger.error("Error in sending delete response", e);
                }
            }).isCancelled()) {
                logger.error("Canceled removing from dao");
            }
            return;
        }

        final var currTime = System.currentTimeMillis();
        request.addHeader(Utility.TIME_HEADER + ": " + currTime);

        final Utility.ReplicationConfiguration parsed =
                AsyncServiceUtility.getReplicationConfiguration(replicas, session, this);
        if (parsed == null) return;

        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        final var futures =
                AsyncServiceUtility.getFuturesReponseDelete(id, currTime, parsed, this, nodes);
        final var homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);

        if (homeInReplicas) {
            futures.add(AsyncServiceUtility.deleteWithTimeStampAsync(key, currTime, this));
        }

        if (Utility.atLeast(parsed.ack, futures).whenCompleteAsync((c, t) -> {
            try {
                if (t == null) {
                    session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                } else {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                }
            } catch (IOException e) {
                AsyncService.logger.error("Error in sending response in delete", e);
            }
        }).isCancelled()) {
            AsyncService.logger.error("Canceled task");
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
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
