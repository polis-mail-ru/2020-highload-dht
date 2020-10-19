package ru.mail.polis.service.zvladn7;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
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
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncService extends HttpServer implements Service {

    private static final String ERROR_SENDING_RESPONSE = "Error when sending response";
    private static final String ERROR_SERVICE_UNAVAILABLE = "Cannot send SERVICE_UNAVAILABLE response";
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    private final DAO dao;
    private final Topology<String> topology;
    private final Cache<String, byte[]> cache;
    private final ExecutorService es;
    private final Map<String, HttpClient> clients;

    /**
     * Asynchronous server implementation.
     *
     * @param port            - server port
     * @param dao             - DAO implemenation
     * @param amountOfWorkers - amount of workers in executor service
     * @param queueSize       - queue size of requests in executor service
     */
    public AsyncService(final int port,
                        @NotNull final DAO dao,
                        final int amountOfWorkers,
                        final int queueSize,
                        final int cacheSize,
                        @NotNull final Topology<String> topology) throws IOException {
        super(provideConfig(port));
        this.dao = dao;
        this.topology = topology;
        this.es = new ThreadPoolExecutor(amountOfWorkers, amountOfWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error when processing request in: {}", t, e)
                        ).build(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.cache = CacheBuilder.newBuilder()
                .initialCapacity(cacheSize)
                .concurrencyLevel(amountOfWorkers)
                .removalListener((RemovalListener<String, byte[]>) notification -> {
                    log.debug("Remove from cache with key: " + notification.getKey());
                    log.debug("Cause: " + notification.getCause().name());
                })
                .maximumSize(cacheSize)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build();

        this.clients = new HashMap<>();
        for (final String node : topology.nodes()) {
            if (topology.isLocal(node)) {
                continue;
            }

            final ConnectionString connectionString = new ConnectionString(node + "?timeout=1000");
            final HttpClient client = new HttpClient(connectionString);
            if (clients.put(node, client) != null) {
                log.error("Cannot start server. Duplicate node with connection string: {}", node);
                throw new IllegalStateException("Duplicate node");
            }
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        log.info("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Return status of the server instance.
     *
     * @return Response - OK if service is available
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("Status: OK");
    }

    /**
     * This method get value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 200 OK. Also return body.
     * 2. 400 if id is empty
     * 3. 404 if value with id was not found
     * 4. 500 if some io error was happened
     *
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id,
                    final HttpSession session,
                    final Request request) {
        processRequest(() -> handleGet(id, session, request), session);
    }

    /**
     * This method delete value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 202 if value is successfully deleted
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void remove(@Param(value = "id", required = true) final String id,
                       final HttpSession session,
                       final Request request) {
        processRequest(() -> handleDelete(id, session, request), session);
    }

    /**
     * This method insert or update value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 201 if value is successfully inserted and created
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void upsert(
            @Param(value = "id", required = true) final String id,
            final Request request,
            final HttpSession session) {
        processRequest(() -> handleUpsert(id, request, session), session);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        es.shutdown();
        try {
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error when trying to stop executor service");
            Thread.currentThread().interrupt();
        }
    }

    private void proxy(@NotNull final String nodeForResponse,
                           @NotNull final Request request,
                           @NotNull final HttpSession session) throws IOException {
        log.info("Proxy request: {} from {} to {}", request.getMethodName(), topology.local(), nodeForResponse);
        try {
            request.addHeader("X-Proxy-For: " + nodeForResponse);
            session.sendResponse(clients.get(nodeForResponse).invoke(request));
        } catch (Exception e) {
            log.error("Cannot proxy request!", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void handleGet(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request) throws IOException {
        log.debug("GET request with mapping: /v0/entity and key={}", id);
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, "GET");
            return;
        }

        final ByteBuffer key = wrapString(id);
        final String nodeForResponse = topology.nodeFor(key);
        if (topology.isLocal(nodeForResponse)) {
            byte[] body = cache.getIfPresent(id);
            if (body == null) {
                log.debug("Not from cache with id: {}", id);
                final ByteBuffer value;
                try {
                    value = dao.get(key);
                } catch (NoSuchElementException e) {
                    log.info("Value with key: {} was not found", id, e);
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    return;
                } catch (IOException e) {
                    sendInternalErrorResponse(session, id, e);
                    return;
                }
                body = toBytes(value);
                cache.put(id, body);
            }
            session.sendResponse(Response.ok(body));
        } else {
            proxy(nodeForResponse, request, session);
        }
    }

    private void handleDelete(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request) throws IOException {
        log.debug("DELETE request with mapping: /v0/entity and key={}", id);
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, "DELETE");
            return;
        }
        final ByteBuffer key = wrapString(id);
        final String nodeForResponse = topology.nodeFor(key);
        if (topology.isLocal(nodeForResponse)) {
            try {
                dao.remove(key);
                cache.invalidate(id);
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } else {
            proxy(nodeForResponse, request, session);
        }
    }

    private void handleUpsert(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final HttpSession session) throws IOException {
        log.debug("PUT request with mapping: /v0/entity with: key={}, value={}",
                id, new String(request.getBody(), StandardCharsets.UTF_8));
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, "UPSERT");
            return;
        }

        final ByteBuffer key = wrapString(id);
        final String nodeForResponse = topology.nodeFor(key);
        if (topology.isLocal(nodeForResponse)) {
            final ByteBuffer value = wrapArray(request.getBody());
            try {
                dao.upsert(key, value);
                cache.asMap().computeIfPresent(id, (k, v) -> request.getBody());
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } else {
            proxy(nodeForResponse, request, session);
        }
    }

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    private static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    private static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }

        return Response.EMPTY;
    }

    private static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    private static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    private static void sendServiceUnavailableResponse(final HttpSession session, final RejectedExecutionException e) {
        log.error("Cannot complete request", e);
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
        } catch (IOException ex) {
            log.error(ERROR_SERVICE_UNAVAILABLE, ex);
        }
    }

    private static void sendEmptyIdResponse(final HttpSession session, final String methodName) throws IOException {
        log.info("Empty key was provided in {} method!", methodName);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private static void sendInternalErrorResponse(final HttpSession session,
                                                  final String id,
                                                  final Exception e) throws IOException {
        log.error("Internal error. Can't insert or update value with key: {}", id, e);
        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }

    private static void process(final Processor processor) {
        try {
            processor.process();
        } catch (IOException e) {
            log.error(ERROR_SENDING_RESPONSE, e);
        }
    }

    private void processRequest(final Processor processor, final HttpSession session) {
        try {
            es.execute(() -> process(processor));
        } catch (RejectedExecutionException e) {
            sendServiceUnavailableResponse(session, e);
        }
    }
}
