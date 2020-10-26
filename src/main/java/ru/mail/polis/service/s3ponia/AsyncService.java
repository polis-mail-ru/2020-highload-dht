package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Table;
import ru.mail.polis.s3ponia.Utility;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.s3ponia.Utility.*;

public final class AsyncService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private final DAO dao;
    private final ExecutorService es;
    private final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;
    private final List<Utility.ReplicationConfiguration> defaultConfigurations = Arrays.asList(
            new Utility.ReplicationConfiguration(1, 1),
            new Utility.ReplicationConfiguration(2, 2),
            new Utility.ReplicationConfiguration(2, 3),
            new Utility.ReplicationConfiguration(3, 4),
            new Utility.ReplicationConfiguration(3, 5)
    );
    
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
        super(configFrom(port));
        assert 0 < workers;
        assert 0 < queueSize;
        this.policy = policy;
        this.urlToClient = urltoClientFromSet(this.policy.all());
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
    
    public ShardingPolicy<ByteBuffer, String> getPolicy() {
        return policy;
    }
    
    public ExecutorService getEs() {
        return es;
    }
    
    public Map<String, HttpClient> getUrlToClient() {
        return urlToClient;
    }
    
    private Map<String, HttpClient> urltoClientFromSet(@NotNull final String... nodes) {
        final Map<String, HttpClient> result = new HashMap<>();
        for (final var url : nodes) {
            if (url.equals(this.policy.homeNode())) {
                continue;
            }
            if (result.put(url, new HttpClient(new ConnectionString(url))) != null) {
                throw new RuntimeException("Duplicated url in nodes.");
            }
        }
        return result;
    }
    
    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[1];
        config.acceptors[0] = ac;
        return config;
    }
    
    @NotNull
    private static byte[] fromByteBuffer(@NotNull final ByteBuffer b) {
        final byte[] out = new byte[b.remaining()];
        b.get(out);
        return out;
    }
    
    @NotNull
    public static AsyncService of(final int port, @NotNull final DAO dao,
                                  final int workers, final int queueSize,
                                  @NotNull final ShardingPolicy<ByteBuffer, String> policy) throws IOException {
        return new AsyncService(port, dao, workers, queueSize, policy);
    }
    
    /**
     * Handling status request.
     *
     * @param session current Session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) throws IOException {
        try {
            this.es.execute(() -> handlingStatusError(session));
        } catch (RejectedExecutionException e) {
            logger.error("Internal error in status handling", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, EMPTY));
        }
    }
    
    private void handlingStatusError(@NotNull final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            logger.error("Error in sending status", e);
        }
    }
    
    private void getRaw(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
        try {
            final var val = dao.getRaw(key);
            final var resp = Response.ok(fromByteBuffer(val.getValue()));
            resp.addHeader(DEADFLAG_TIMESTAMP_HEADER + ": " + val.getDeadFlagTimeStamp());
            session.sendResponse(resp);
        } catch (NoSuchElementException noSuchElementException) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in getting key(size: {}) from dao", key.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }
    
    private static long getDeadFlagTimeStamp(@NotNull final Response response) {
        final var header = Utility.Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        assert header != null;
        return Long.parseLong(header.value);
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
        if (validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        final var key = byteBufferFromString(id);
        if (request.getHeader(PROXY_HEADER) != null) {
            this.es.execute(() -> {
                try {
                    getRaw(key, session);
                } catch (IOException ioException) {
                    logger.error("Error in raw getting.", ioException);
                }
            });
            return;
        }
        final Utility.ReplicationConfiguration parsed = getReplicationConfiguration(replicas, session);
        if (parsed == null) return;
        
        final var nodeReplicas = policy.getNodeReplicas(key, parsed.from);
        final List<Table.Value> values = getValues(request, parsed, nodeReplicas);
        boolean homeInReplicas = isHomeInReplicas(nodeReplicas);
        
        if (values.size() + (homeInReplicas ? 1 : 0) < parsed.ack) {
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
            return;
        }
        
        if (homeInReplicas) {
            try {
                values.add(dao.getRaw(key));
            } catch (NoSuchElementException ignored) {
            }
        }
        
        if (values.isEmpty()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
            return;
        }
        
        values.sort(Comparator.comparing(Table.Value::getTimeStamp)
                            .reversed()
                            .thenComparing(deadComparator())
        );
        
        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
        } else {
            session.sendResponse(Response.ok(fromByteBuffer(bestVal.getValue())));
        }
    }
    
    @NotNull
    private Comparator<Table.Value> deadComparator() {
        return (a, b) -> {
            if (a.isDead()) {
                return -1;
            }
            if (b.isDead()) {
                return 1;
            }
            
            return 0;
        };
    }
    
    private boolean isHomeInReplicas(String[] nodeReplicas) {
        boolean homeInReplicas = false;
        
        for (final var node :
                nodeReplicas) {
            if (node.equals(policy.homeNode())) {
                homeInReplicas = true;
            }
        }
        return homeInReplicas;
    }
    
    @NotNull
    private List<Table.Value> getValues(@NotNull final Request request,
                                        @NotNull final Utility.ReplicationConfiguration parsed,
                                        @NotNull final String... nodeReplicas) {
        final List<Future<Response>> futureResponses = getFutures(request, parsed, this, nodeReplicas);
        return getValuesFromFutures(parsed, futureResponses);
    }
    
    @NotNull
    private List<Table.Value> getValuesFromFutures(@NotNull final Utility.ReplicationConfiguration parsed,
                                                   @NotNull final List<Future<Response>> futureResponses) {
        final List<Table.Value> values = new ArrayList<>(parsed.from);
        for (final var resp :
                futureResponses) {
            final Response response;
            try {
                response = resp.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error in proxing");
                continue;
            }
            if (response != null && response.getStatus() == 200 /* OK */) {
                final var val = Table.Value.of(ByteBuffer.wrap(response.getBody()),
                        getDeadFlagTimeStamp(response), -1);
                values.add(val);
            }
        }
        return values;
    }
    
    private Utility.ReplicationConfiguration parseAndValidateReplicas(final String replicas) {
        final Utility.ReplicationConfiguration parsedReplica;
        final var nodeCount = this.policy.all().length;
        
        parsedReplica = replicas != null ? Utility.ReplicationConfiguration.parse(replicas) :
                                defaultConfigurations.get(nodeCount - 1);
        
        if (parsedReplica == null || parsedReplica.ack <= 0
                    || parsedReplica.ack > parsedReplica.from || parsedReplica.from > nodeCount) {
            return null;
        }
        
        return parsedReplica;
    }
    
    /**
     * Process upserting to dao.
     *
     * @param key     key for upserting
     * @param value   value for upserting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    private void upsertWithTimeStamp(@NotNull final ByteBuffer key,
                                     @NotNull final ByteBuffer value,
                                     @NotNull final HttpSession session,
                                     final long timeStamp) throws IOException {
        try {
            dao.upsertWithTimeStamp(key, value, timeStamp);
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in putting key(size: {}), value(size: {}) from dao",
                    key.capacity(), value.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
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
        if (validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        final var key = byteBufferFromString(id);
        final var value = ByteBuffer.wrap(request.getBody());
        
        final var header = Utility.Header.getHeader(TIME_HEADER, request);
        if (header != null) {
            final var time = Long.parseLong(header.value);
            this.es.execute(
                    () -> {
                        try {
                            upsertWithTimeStamp(key, value, session, time);
                        } catch (IOException ioException) {
                            logger.error("Error in sending put request", ioException);
                        }
                    }
            );
            return;
        }
        
        final Utility.ReplicationConfiguration parsed = getReplicationConfiguration(replicas, session);
        if (parsed == null) return;
        
        final var currTime = System.currentTimeMillis();
        request.addHeader(TIME_HEADER + ": " + currTime);
        
        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        int createdCounter = getCounter(request, parsed, this, nodes);
        
        final boolean homeInReplicas = isHomeInReplicas(nodes);
        
        if (homeInReplicas) {
            try {
                dao.upsertWithTimeStamp(key, value, currTime);
                ++createdCounter;
            } catch (IOException ioException) {
                logger.error("IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                        key.capacity(), value.capacity(), this.policy.homeNode(), ioException);
            }
        }
        
        sendAckFromResp(session, parsed, createdCounter, new Response(Response.CREATED, EMPTY), "Error in putting");
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
        if (validateId(id)) {
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        final var key = byteBufferFromString(id);
        final var header = Utility.Header.getHeader(TIME_HEADER, request);
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
        request.addHeader(TIME_HEADER + ": " + currTime);
        
        final Utility.ReplicationConfiguration parsed = getReplicationConfiguration(replicas, session);
        if (parsed == null) return;
        
        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        int acceptedCounter = getCounter(request, parsed, this, nodes);
        final var homeInReplicas = isHomeInReplicas(nodes);
        
        if (homeInReplicas) {
            try {
                dao.removeWithTimeStamp(key, currTime);
                ++acceptedCounter;
            } catch (IOException ioException) {
                logger.error("IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                        key.capacity(), request.getBody().length, this.policy.homeNode(), ioException);
            }
        }
        
        sendAckFromResp(session, parsed, acceptedCounter,
                new Response(Response.ACCEPTED, EMPTY), "Error in sending resp");
    }
    
    @Nullable
    private Utility.ReplicationConfiguration getReplicationConfiguration(
            @NotNull final String replicas,
            @NotNull final HttpSession session) throws IOException {
        final var parsed = parseAndValidateReplicas(replicas);
        
        if (parsed == null) {
            logger.error("Bad replicas param {}", replicas);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return null;
        }
        return parsed;
    }
    
    private void sendAckFromResp(@NotNull final HttpSession session,
                                 @NotNull final Utility.ReplicationConfiguration parsed,
                                 final int acceptedCounter,
                                 final Response resp,
                                 @NotNull final String s) {
        if (acceptedCounter >= parsed.ack) {
            this.es.execute(
                    () -> {
                        try {
                            session.sendResponse(resp);
                        } catch (IOException ioException) {
                            logger.error(s, ioException);
                        }
                    }
            );
        } else {
            this.es.execute(
                    () -> {
                        try {
                            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
                        } catch (IOException ioException) {
                            logger.error("Error in sending error", ioException);
                        }
                    }
            );
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
