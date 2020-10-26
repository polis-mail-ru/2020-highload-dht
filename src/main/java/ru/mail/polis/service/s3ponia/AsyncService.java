package ru.mail.polis.service.s3ponia;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Table;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

public final class AsyncService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private static final String DEADFLAG_TIMESTAMP_HEADER = "XDeadFlagTimestamp";
    private static final String PROXY_HEADER = "X-Proxy-From";
    private static final String TIME_HEADER = "XTime";
    private final DAO dao;
    private final ExecutorService es;
    private final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;
    private final List<ReplicationConfiguration> defaultConfigurations = Arrays.asList(
            new ReplicationConfiguration(1, 1),
            new ReplicationConfiguration(2, 2),
            new ReplicationConfiguration(2, 3),
            new ReplicationConfiguration(3, 4),
            new ReplicationConfiguration(3, 5)
    );
    
    private static class Header {
        final String key;
        final String value;
        
        public Header(@NotNull final String key, @NotNull final String value) {
            this.key = key;
            this.value = value;
        }
        
        public static Header getHeader(@NotNull final String key, @NotNull final Request response) {
            final var headers = response.getHeaders();
            final var headerCount = response.getHeaderCount();
            int keyLength = key.length();
            
            for (int i = 1; i < headerCount; ++i) {
                if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                    final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                    return new Header(headers[i], value);
                }
            }
            
            return null;
        }
        
        public static Header getHeader(@NotNull final String key, @NotNull final Response response) {
            final var headers = response.getHeaders();
            final var headerCount = response.getHeaderCount();
            int keyLength = key.length();
            
            for (int i = 1; i < headerCount; ++i) {
                if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                    final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                    return new Header(headers[i], value);
                }
            }
            
            return null;
        }
    }
    
    private static class ReplicationConfiguration {
        final int ack;
        final int from;
        
        public ReplicationConfiguration(final int ack, final int from) {
            this.ack = ack;
            this.from = from;
        }
        
        public static ReplicationConfiguration parse(@NotNull final String s) {
            final var splitStrings = Splitter.on('/').splitToList(s);
            if (splitStrings.size() != 2) {
                return null;
            }
            
            try {
                return new ReplicationConfiguration(Integer.parseInt(splitStrings.get(0)),
                        Integer.parseInt(splitStrings.get(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
    
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
    
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) throws IOException {
        try {
            request.addHeader(PROXY_HEADER + ":" + node);
            return this.urlToClient.get(node).invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            logger.error("Can't proxy request", exception);
            return null;
        }
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
    
    /**
     * Process getting from dao.
     *
     * @param key     key for getting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void get(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
        try {
            session.sendResponse(Response.ok(fromByteBuffer(dao.get(key))));
        } catch (NoSuchElementException noSuchElementException) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in getting key(size: {}) from dao", key.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }
    
    public void getRaw(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
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
    
    public static long getDeadFlagTimeStamp(@NotNull final Response response) {
        final var header = Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        assert header != null;
        return Long.parseLong(header.value);
    }
    
    public static boolean isDead(@NotNull final Response response) {
        final var header = Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        assert header != null;
        final var deadTimestamp = Long.parseLong(header.value);
        return Table.Value.isDead(deadTimestamp);
    }
    
    public static long getTimeStamp(@NotNull final Response response) {
        final var header = Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        assert header != null;
        final var deadTimestamp = Long.parseLong(header.value);
        return Table.Value.getTimeStampFromLong(deadTimestamp);
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
        if (validateId(id, session, "Invalid id in get")) return;
        
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
        final var parsed = parseAndValidateReplicas(replicas);
        
        if (parsed == null) {
            logger.error("Bad replicas param {}", replicas);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        int ackCounter = 0;
        final var nodeReplicas = policy.getNodeReplicas(key, parsed.from);
        final List<Future<Response>> futureResponses = new ArrayList<>(parsed.from);
        final List<Table.Value> values = new ArrayList<>(parsed.from);
        
        for (final var node :
                nodeReplicas) {
            if (!node.equals(policy.homeNode())) {
                futureResponses.add(this.es.submit(() -> proxy(node, request)));
            } else {
                ++ackCounter;
                try {
                    values.add(dao.getRaw(key));
                } catch (NoSuchElementException ignored) {
                }
            }
        }
        
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
                ++ackCounter;
                final var val = Table.Value.of(ByteBuffer.wrap(response.getBody()),
                        getDeadFlagTimeStamp(response), -1);
                values.add(val);
            }
        }
        
        if (ackCounter < parsed.ack) {
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, EMPTY));
            return;
        }
        
        if (values.isEmpty()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
            return;
        }
        
        values.sort(Comparator.comparing(Table.Value::getTimeStamp)
                            .reversed()
                            .thenComparing(
                                    (a, b) -> {
                                        if (a.isDead()) {
                                            return -1;
                                        }
                                        if (b.isDead()) {
                                            return 1;
                                        }
                            
                                        return 0;
                                    })
        );
        
        final var bestVal = values.get(0);
        if (bestVal.isDead()) {
            session.sendResponse(new Response(Response.NOT_FOUND, EMPTY));
        } else {
            session.sendResponse(Response.ok(fromByteBuffer(bestVal.getValue())));
        }
    }
    
    public ReplicationConfiguration parseAndValidateReplicas(final String replicas) throws IOException {
        final ReplicationConfiguration parsedReplica;
        final var nodeCount = this.policy.all().length;
        
        if (replicas != null) {
            parsedReplica = ReplicationConfiguration.parse(replicas);
        } else {
            parsedReplica = defaultConfigurations.get(nodeCount - 1);
        }
        
        if (parsedReplica == null || parsedReplica.ack <= 0
                    || parsedReplica.ack > parsedReplica.from || parsedReplica.from > nodeCount) {
            return null;
        }
        
        return parsedReplica;
    }
    
    private boolean validateId(@NotNull final String id,
                               @NotNull final HttpSession session,
                               @NotNull final String s) throws IOException {
        if (id.isEmpty()) {
            logger.error(s);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return true;
        }
        return false;
    }
    
    /**
     * Process upserting to dao.
     *
     * @param key     key for upserting
     * @param value   value for upserting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       @NotNull final HttpSession session) throws IOException {
        try {
            dao.upsert(key, value);
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in putting key(size: {}), value(size: {}) from dao",
                    key.capacity(), value.capacity(), ioException);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }
    
    /**
     * Process upserting to dao.
     *
     * @param key     key for upserting
     * @param value   value for upserting
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void upsertWithTimeStamp(@NotNull final ByteBuffer key,
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
    
    private ByteBuffer byteBufferFromString(@NotNull final String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
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
        final var currTime = System.currentTimeMillis();
        if (validateId(id, session, "Invalid id in put")) return;
        
        final var key = byteBufferFromString(id);
        final var value = ByteBuffer.wrap(request.getBody());
        
        final var header = Header.getHeader(TIME_HEADER, request);
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
        
        final var parsed = parseAndValidateReplicas(replicas);
        
        if (parsed == null) {
            logger.error("Bad replicas param {}", replicas);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        int createdCounter = 0;
        
        request.addHeader(TIME_HEADER + ": " + currTime);
        
        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        final List<Future<Response>> futureResponses = new ArrayList<>(parsed.from);
        
        for (final var node :
                nodes) {
            
            if (!node.equals(policy.homeNode())) {
                futureResponses.add(this.es.submit(() -> proxy(node, request)));
            } else {
                try {
                    dao.upsertWithTimeStamp(key, value, currTime);
                    ++createdCounter;
                } catch (IOException ioException) {
                    logger.error("IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                            key.capacity(), value.capacity(), this.policy.homeNode(), ioException);
                }
            }
        }
        
        for (final var resp :
                futureResponses) {
            final Response response;
            try {
                response = resp.get();
            } catch (InterruptedException | ExecutionException e) {
                continue;
            }
            if (response != null && response.getStatus() == 201 /* CREATED */) {
                ++createdCounter;
            }
        }
        
        if (createdCounter >= parsed.ack) {
            this.es.execute(
                    () -> {
                        try {
                            session.sendResponse(new Response(Response.CREATED, EMPTY));
                        } catch (IOException ioException) {
                            logger.error("Error in putting", ioException);
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
    
    /**
     * Process deleting from dao.
     *
     * @param key     record's key to delete
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void delete(@NotNull final ByteBuffer key, @NotNull final HttpSession session) throws IOException {
        try {
            dao.remove(key);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("IOException in removing key(size: {}) from dao", key.capacity());
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }
    
    /**
     * Process deleting from dao.
     *
     * @param key     record's key to delete
     * @param session HttpSession for response
     * @throws IOException rethrow from sendResponse
     */
    public void deleteWithtimeStamp(@NotNull final ByteBuffer key,
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
        if (validateId(id, session, "Invalid id in put")) return;
        
        final var key = byteBufferFromString(id);
        
        final var header = Header.getHeader(TIME_HEADER, request);
        if (header != null) {
            this.es.execute(
                    () -> {
                        try {
                            deleteWithtimeStamp(key, session, Long.parseLong(header.value));
                        } catch (IOException ioException) {
                            logger.error("Error in sending put request", ioException);
                        }
                    }
            );
            return;
        }
        
        final var parsed = parseAndValidateReplicas(replicas);
        
        if (parsed == null) {
            logger.error("Bad replicas param {}", replicas);
            session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
            return;
        }
        
        int acceptedCounter = 0;
        final var currTime = System.currentTimeMillis();
        
        request.addHeader(TIME_HEADER + ": " + currTime);
        
        final var nodes = this.policy.getNodeReplicas(key, parsed.from);
        final List<Future<Response>> futureResponses = new ArrayList<>(parsed.from);
        
        for (final var node :
                nodes) {
            
            if (!node.equals(policy.homeNode())) {
                futureResponses.add(this.es.submit(() -> proxy(node, request)));
            } else {
                try {
                    dao.removeWithTimeStamp(key, currTime);
                    ++acceptedCounter;
                } catch (IOException ioException) {
                    logger.error("IOException in putting key(size: {}), value(size: {}) from dao on node {}",
                            key.capacity(), request.getBody().length, this.policy.homeNode(), ioException);
                }
            }
        }
        
        for (final var resp :
                futureResponses) {
            final Response response;
            try {
                response = resp.get();
            } catch (InterruptedException | ExecutionException e) {
                continue;
            }
            if (response != null && response.getStatus() == 202 /* ACCEPTED */) {
                ++acceptedCounter;
            }
        }
        
        if (acceptedCounter >= parsed.ack) {
            this.es.execute(
                    () -> {
                        try {
                            session.sendResponse(new Response(Response.ACCEPTED, EMPTY));
                        } catch (IOException ioException) {
                            logger.error("Error in putting", ioException);
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
