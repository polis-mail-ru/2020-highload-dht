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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
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
            this.es.execute(() -> {
                try {
                    AsyncServiceUtility.getRaw(key, session, this.dao);
                } catch (IOException ioException) {
                    logger.error("Error in raw getting.", ioException);
                }
            });
            return;
        }
        final Utility.ReplicationConfiguration parsed =
                AsyncServiceUtility.getReplicationConfiguration(replicas, session, this);
        if (parsed == null) return;
        
        final var nodeReplicas = policy.getNodeReplicas(key, parsed.from);
        final List<Table.Value> values = AsyncServiceUtility.getValues(request, parsed, this, nodeReplicas);
        final boolean homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodeReplicas);
        
        if (values.size() + (homeInReplicas ? 1 : 0) < parsed.ack) {
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
            return;
        }
        
        if (homeInReplicas) {
            try {
                values.add(dao.getRaw(key));
            } catch (NoSuchElementException exception) {
                logger.error("Error in getting key(size : {})", key.capacity(), exception);
            }
        }
        
        if (values.isEmpty()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
            return;
        }
        
        values.sort(Utility.valueResponseComparator());
        
        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
        } else {
            session.sendResponse(Response.ok(Utility.fromByteBuffer(bestVal.getValue())));
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
            this.es.execute(() -> AsyncServiceUtility.putImpl(id, replicas, request, session, this));
        } catch (RejectedExecutionException e) {
            logger.error("Error in execute", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }
    
    /**
     * Process deleting from dao.
     *
     * @param key     record's key to delete
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    private void deleteWithTimeStamp(@NotNull final ByteBuffer key,
                                     @NotNull final HttpSession session,
                                     final long timeStamp) throws IOException {
        try {
            dao.removeWithTimeStamp(key, timeStamp);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in removing key(size: {}) from dao", key.capacity());
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
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
            this.es.execute(
                    () -> {
                        try {
                            deleteWithTimeStamp(key, session, Long.parseLong(header.value));
                        } catch (IOException ioException) {
                            logger.error("Error in sending put request", ioException);
                        }
                    }
            );
            return;
        }
        
        final var currTime = System.currentTimeMillis();
        request.addHeader(Utility.TIME_HEADER + ": " + currTime);
        
        final Utility.ReplicationConfiguration parsed =
                AsyncServiceUtility.getReplicationConfiguration(replicas, session, this);
        if (parsed == null) return;
        
        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        int acceptedCounter = AsyncServiceUtility.getCounter(request, parsed, this, nodes);
        final var homeInReplicas = Utility.isHomeInReplicas(policy.homeNode(), nodes);
        
        if (homeInReplicas) {
            try {
                dao.removeWithTimeStamp(key, currTime);
                ++acceptedCounter;
            } catch (IOException ioException) {
                logger.error("IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                        key.capacity(), request.getBody().length, this.policy.homeNode(), ioException);
            }
        }
        
        AsyncServiceUtility.sendAckFromResp(parsed, acceptedCounter,
                new Response(Response.ACCEPTED, EMPTY), session);
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
