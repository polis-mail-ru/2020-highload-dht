package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.util.Util.toByteArray;

public class ServiceAsyncImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAsyncImpl.class);
    private static final String PROXY_HEADER = "X-Proxy-For:";
    private static final int CONNECTION_TIMEOUT = 100;

    @NotNull
    private final DAO dao;

    @NotNull
    private final ExecutorService executor;

    private final Topology topology;
    private final Map<String, HttpClient> nodeToClient;

    ServiceAsyncImpl(
            final int port,
            @NotNull final DAO dao,
            final int workersCount,
            final int queueSize,
            @NotNull final Topology topology
    ) throws IOException {
        super(getConfig(port, workersCount));
        this.dao = dao;
        this.executor = new ThreadPoolExecutor(
                workersCount, workersCount,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                        .setNameFormat("async_worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.topology = topology;
        final Map<String, HttpClient> clientMap = new HashMap<>();
        for (final String node : topology.getNodes()) {
            if (!topology.isSelfId(node) && !clientMap.containsKey(node)) {
                clientMap.put(node, new HttpClient(
                        new ConnectionString(String.format("%s?timeout=%d", node, CONNECTION_TIMEOUT))
                ));
            }
        }
        this.nodeToClient = clientMap;
    }

    /**
     * Standard response for successful HTTP requests.
     *
     * @return HTTP status code 200 (OK)
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Provide access to entities.
     *
     * @param id      key of entity
     * @param request HTTP request
     * @param session HTTP session
     */
    @Path("/v0/entity")
    public void entity(
            @Param("id") final String id,
            @NotNull final Request request,
            @NotNull final HttpSession session
    ) {
        try {
            if (id == null || id.isEmpty()) {
                session.sendResponse(
                    new Response(Response.BAD_REQUEST, Response.EMPTY)
                );
                return;
            }

            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charset.defaultCharset()));
            final String keyClusterPartition = topology.primaryFor(key);

            if (!topology.isSelfId(keyClusterPartition)) {
                executor.execute(() -> {
                    forwardRequest(keyClusterPartition, request, session);
                });
                return;
            }

            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    get(key, session);
                    break;
                case Request.METHOD_PUT:
                    put(key, request, session);
                    break;
                case Request.METHOD_DELETE:
                    delete(key, session);
                    break;
                default:
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    logger.error("Non-supported request type: {}", request.getMethod());
                    break;
            }
        } catch (IOException ex) {
            this.handleError(session);
        }
    }

    private void handleError(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        } catch (IOException e) {
            logger.error("Couldn't send response", e);
        }
    }

    private void get(final ByteBuffer key, final HttpSession session) {
        try {
            this.executor.execute(() -> {
                try {
                    getValue(key, session);
                } catch (IOException e) {
                    logger.error("Couldn't send response", e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.error("Execution exception in GET", e);
        }
    }

    private void getValue(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            session.sendResponse(new Response(Response.OK, toByteArray(dao.get(key))));
        } catch (NoSuchElementException ex) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            logger.error("Element not found", ex);
        } catch (IOException e) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            logger.error("GET error:", e);
        }
    }

    private void putValue(final ByteBuffer key, final ByteBuffer value, final HttpSession session) throws IOException {
        try {
            dao.upsert(key, value);
        } catch (IOException e) {
            this.handleError(session);
            return;
        }
        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    private void put(
        final ByteBuffer key,
        final Request request,
        final HttpSession session
    ) {
        try {
            this.executor.execute(() -> {
                try {
                    this.putValue(key, ByteBuffer.wrap(request.getBody()), session);
                } catch (IOException e) {
                    logger.error("PUT error:", e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.error("Execution exception in PUT", e);
        }
    }

    private void delete(final ByteBuffer key, final HttpSession session) {
        try {
            this.executor.execute(() -> {
                try {
                    dao.remove(key);
                    session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                } catch (IOException e) {
                    this.handleError(session);
                    logger.error("DELETE error:", e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.error("Execution exception in DELETE", e);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    private static HttpServerConfig getConfig(final int port, final int workersCount) {
        final int portMin = 1024;
        final int portMax = 65536;
        if (port <= portMin || portMax <= port) {
            throw new IllegalArgumentException(
                    String.format("Invalid port value provided. It must be between %d and %d", portMin, portMax)
            );
        }

        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig serverConfig = new HttpServerConfig();
        serverConfig.maxWorkers = workersCount;
        serverConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return serverConfig;
    }

    /**
     * This forwards request to other nodes in the cluster.
     * @param nodeId - id of target node
     * @param request - request to forward
     * @return response from forwarded request
     */
    private Response proxy(@NotNull final String nodeId,
                           @NotNull final Request request) {
        try {
            request.addHeader(String.format("%s %s", PROXY_HEADER, nodeId));
            return nodeToClient.get(nodeId).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException exc) {
            logger.error("Error sending request via proxy", exc);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void forwardRequest(
            final String keyClusterPartition,
            final Request request,
            final HttpSession session
    ) {
        try {
            final Response response = proxy(keyClusterPartition, request);
            session.sendResponse(response);
        } catch (IOException e) {
            this.handleError(session);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown execution");
            Thread.currentThread().interrupt();
        }
    }
}
