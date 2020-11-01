package ru.mail.polis.service.kovalkov;

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
import one.nio.pool.Pool;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.kovalkov.replication.ReplicationController;
import ru.mail.polis.service.kovalkov.replication.ReplicationFactor;
import ru.mail.polis.service.kovalkov.sharding.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ReplicationServiceImpl extends HttpServer implements Service {
    private static final String IO_EX = "IO exception. Internal error response";
    private static final Logger log = LoggerFactory.getLogger(ReplicationServiceImpl.class);
    private final Map<String, HttpClient> nodesClient = new HashMap<>();
    private final Topology<String> topology;
    private final ExecutorService service;
    private final DAO dao;
    private final ReplicationFactor replFactor;
    private final ReplicationController controller;

    /**
     * Constructor.
     * @param config - service configuration.
     * @param dao - dao implementation.
     * @param topology  - cluster configuration
     */
    public ReplicationServiceImpl(final HttpServerConfig config,
                                  final DAO dao, final Topology<String> topology) throws IOException {
        super(config);
        this.dao = dao;
        this.topology = topology;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in async_worker-{} : ", t, e))
                        .setNameFormat("async_worker-%d")
                        .build());
        for (final String n: topology.allNodes()) {
            if (!topology.isMe(n) && !this.nodesClient.containsKey(n)) {
                final HttpClient client = new HttpClient(new ConnectionString(n + "?timeout=1000"));
                this.nodesClient.put(n, client);
            }
        }
        this.replFactor = new ReplicationFactor(topology.nodeCount() / 2 + 1, topology.nodeCount());
        this.controller = new ReplicationController(dao, topology, nodesClient);
    }

    /**
     * Server configuration.
     *
     * @return - return HttpServerConfig
     */
    public static HttpServerConfig getConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.queueTime = 10;
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    /**
     * Check status.
     *
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Proxy nodes.
     *
     * @param targetNode - nodes who have target key
     * @param request - request from user or target node
     * @return - response
     */
    @NotNull
    private Response proxy(@NotNull final String targetNode, @NotNull final Request request) {
        try {
            request.addHeader("X-Proxy-For: " + targetNode);
            final HttpClient client = nodesClient.get(targetNode);
            return client.invoke(request);
        } catch (IOException | HttpException | InterruptedException | PoolException e) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * Forwarding requests using proxy.
     *
     * @param request - request from user or target node
     * @param session - current connection
     * @param owner - nodes who have target key
     */
    private void proxyForwarding(@NotNull final Request request,
                                 @NotNull final HttpSession session, @NotNull final String owner) {
        service.execute(() -> {
            try {
                final Response response = proxy(owner, request);
                session.sendResponse(response);
            } catch (IOException e) {
                log.error("IO exception. Proxy", e);
            }
        });
    }

    /**
     * Get, Put, Delete etc.
     *
     * @param id - key.
     * @param session - current session
     * @param request - request from client or other node
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id",required = true) @NotNull final String id, @NotNull final Request request,
                       @NotNull final HttpSession session, @Param("replicas") final String replicas)  {
        try {
            checkIdAndReturnTargetNode(id, session);
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
            final boolean isForwarded = request.getHeader("X-OK-Proxy: True") != null;
            final ReplicationFactor replicationFactor = ReplicationFactor
                    .getReplicationFactor(replicas, replFactor, session);
            if (topology.nodeCount() == 1) {
                singleNodeExec(request, session, key, id);
            } else {
                noSingleNodeExec(request, session, id, isForwarded, replicationFactor);
            }
        } catch (IOException e) {
            exceptionIOHandler(session, "IO in entity has been occurred", e);
        }
    }

    private void singleNodeExec(@NotNull final Request request,
                                @NotNull final HttpSession session,
                                @NotNull final ByteBuffer key, @NotNull final String id) throws IOException {
        final String ownerNode = topology.identifyByKey(ByteBuffer.wrap(id.getBytes(UTF_8)));
        if (topology.isMe(ownerNode)) {
            switch (request.getMethod()) {
                case METHOD_GET:
                    asyncGet(key, session);
                    break;
                case METHOD_PUT:
                    asyncPut(key, request, session);
                    break;
                case METHOD_DELETE:
                    asyncDelete(key, session);
                    break;
                default:
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    log.info("Unsupported method");
                    break;
            }
        } else {
            proxyForwarding(request, session, ownerNode);
        }
    }

    private void noSingleNodeExec(@NotNull final Request request, @NotNull final HttpSession session,
                                  @NotNull String id, final boolean isForwarded,
                                  @NotNull final ReplicationFactor replicationFactor) throws IOException {
        final String ownerNode = topology.identifyByKey(ByteBuffer.wrap(id.getBytes(UTF_8)));
        if (topology.isMe(ownerNode)) {
            switch (request.getMethod()) {
                case METHOD_GET:
                    service.execute(() -> {
                        try {
                            session.sendResponse(controller.replGet(id, replicationFactor, isForwarded));
                        } catch (IOException e) {
                            exceptionIOHandler(session, "IO exception in repl get", e);
                        }
                    });
                    break;
                case METHOD_PUT:
                        service.execute(() -> {
                            try {
                                session.sendResponse(controller.replPut(id,
                                        replicationFactor, isForwarded, request.getBody(), replicationFactor.getAck()));
                            } catch (IOException e) {
                                exceptionIOHandler(session, "IO exception in repl put", e);
                            }
                        });
                    break;
                case METHOD_DELETE:
                    service.execute(() -> {
                        try {
                            session.sendResponse(controller.replDelete(id, replicationFactor,
                                    isForwarded, replicationFactor.getAck()));
                        } catch (IOException e) {
                            exceptionIOHandler(session, "IO exception in repl delete", e);
                        }
                    });
                    break;
                default:
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    log.info("Unsupported method");
                    break;
            }
        } else {
            proxyForwarding(request, session, ownerNode);
        }
    }

    private void exceptionIOHandler(final HttpSession session, final String message, final Exception e) {
        log.error(message, e);
        try {
            session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } catch (IOException ex) {
            log.error(IO_EX, ex);
        }
    }

    private void asyncGet(@NotNull final ByteBuffer id, @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                getInternal(id, session);
            } catch (IOException e) {
                exceptionIOHandler(session, "Method get. IO exception. ", e);
            } catch (RejectedExecutionException e) {
                log.error("Rejected  exception: ", e);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException e1) {
                    log.error("IO exception when send 503 response: ", e1);
                }
            } catch (NoSuchElementException e) {
                log.info("Method get. Can't find value by this key ", e);
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException ioException) {
                    log.error("Method get. Id is empty. Can't send response:", e);
                }
            }
        });
    }

    private void getInternal(@NotNull final ByteBuffer key,
                             @NotNull final HttpSession session) throws IOException {
        final ByteBuffer value = dao.get(key);
        final byte[] bytes = BufferConverter.unfoldToBytes(value);
        session.sendResponse(Response.ok(bytes));
    }

    private void asyncPut(@NotNull final ByteBuffer id,
                          @NotNull final Request request, @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                putInternal(id, request, session);
            } catch (IOException e) {
                exceptionIOHandler(session, "IO exception. Put ", e);
            }
        });
    }

    private void putInternal(@NotNull final ByteBuffer key,
                             @NotNull final Request request,
                             @NotNull final HttpSession session) throws IOException {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        dao.upsert(key, value);
        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    private void asyncDelete(@NotNull final ByteBuffer id, @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                deleteInternal(id, session);
            } catch (IOException e) {
                exceptionIOHandler(session, "Method delete. IO exception. ", e);
            }
        });
    }

    private void deleteInternal(@NotNull final ByteBuffer key,
                                @NotNull final HttpSession session) throws IOException {
        dao.remove(key);
        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
    }

    /**
     * Checking id, and get data owner.
     *
     * @param id - key of data
     * @param session - current session
     * @return - data owner
     */
    private void checkIdAndReturnTargetNode(final String id, @NotNull final HttpSession session) throws IOException {
        if (id.isEmpty()) {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        service.shutdown();
        try {
            this.service.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("AwaitTerminations has been interrupted : ", e);
            Thread.currentThread().interrupt();
        }
        nodesClient.values().forEach(Pool::close);
    }
}
