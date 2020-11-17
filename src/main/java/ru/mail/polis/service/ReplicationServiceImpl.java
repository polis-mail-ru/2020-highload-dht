package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;

public class ReplicationServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ReplicationServiceImpl.class);
    static final String FORWARD_REQUEST_HEADER = "X-OK-Proxy: True";
    static final String GATEWAY_TIMEOUT_ERROR_LOG = "Your request failed due to timeout";
    private static final int CONNECTION_TIMEOUT = 1000;

    private enum ErrorNames {
        IO_ERROR, NOT_ALLOWED_METHOD_ERROR, REJECTED, NOT_ENOUGH_NODES, BAD_RANGE_PARAMS
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.NOT_ALLOWED_METHOD_ERROR, "Method not allowed"),
            entry(ErrorNames.REJECTED, "RejectedExecutionException when handling replicas"),
            entry(ErrorNames.NOT_ENOUGH_NODES, "Not enough nodes in cluster"),
            entry(ErrorNames.BAD_RANGE_PARAMS, "Bad range request parameters")
    );

    private final ExecutorService exec;
    private final Topology topology;
    private final Map<String, HttpClient> nodesToClients;
    private final ReplicationHandler handler;
    private final DAO dao;

    ReplicationServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final int workerPoolSize,
            final int queueSize,
            @NotNull final Topology topology
    ) throws IOException {

        super(getConfig(port));
        this.exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.dao = dao;
        this.topology = topology;
        this.nodesToClients = new HashMap<>();
        this.handler = new ReplicationHandler(dao, topology, nodesToClients, ReplicationFactor.getQuorum(
                topology.getSize())
        );

        for (final String node : topology.getNodes()) {

            if (topology.isSelfId(node)) {
                continue;
            }

            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=" + CONNECTION_TIMEOUT));
            if (nodesToClients.put(node, client) != null) {
                throw new IllegalStateException("Found multiple nodes with same ID");
            }
        }
    }

    /**
     * handles formation request to inform client the server is alive and ready to exchange.
     *
     * @param session - ongoing session instance
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
        session.sendResponse(Response.ok("Server is running..."));
    }

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StreamSession(socket, this);
    }

    /**
     * Handles range requests from "start" to "end".
     *
     * @param startId - search from
     * @param endId   - search to
     * @param session - session
     */
    @Path("/v0/entities")
    public void entities(
            @Param(value = "start", required = true) final String startId,
            @Param(value = "end") final String endId,
            final HttpSession session
    ) {
        try {
            if (startId.isEmpty() || ((endId != null) && endId.isEmpty())) {
                throw new IllegalArgumentException(MESSAGE_MAP.get(ErrorNames.BAD_RANGE_PARAMS));
            }

            final ByteBuffer start = ByteBuffer.wrap(startId.getBytes(UTF_8));
            final ByteBuffer end = (endId == null) ? null
                    : ByteBuffer.wrap(endId.getBytes(UTF_8));
            final Iterator<Record> iterator = dao.range(start, end);
            ((StreamSession) session).setIterator(iterator);
        } catch (IOException ex) {
            trySendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY), session);
        } catch (IllegalArgumentException ex) {
            trySendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY), session);
        }
    }

    private void trySendResponse(final Response response, final HttpSession session) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), e);
        }
    }

    /**
     * resolves request handling by HTTP REST methods, provides any client with response (incl. server outcome code).
     *
     * @param id      - String object to be processed as a key in terms of data storage design
     * @param req     - HTTP request
     * @param session - ongoing session instance
     */
    @Path("/v0/entity")
    public void entity(
            @Param(value = "id", required = true) final String id,
            @NotNull final Request req,
            @NotNull final HttpSession session,
            @Param("replicas") final String replicas
    ) throws IOException {

        if (id.isEmpty()) {
            session.sendError(
                    Response.BAD_REQUEST, "Identifier is required as parameter. Error handling request"
            );
            return;
        }
        
        final ReplicationFactor replicationFactor;

        try {
            replicationFactor = replicas == null ? ReplicationFactor.getQuorum(topology.getSize()) :
                    ReplicationFactor.createReplicationFactor(
                            replicas
                    );
        } catch (IllegalArgumentException ex) {
            session.sendError(Response.BAD_REQUEST, ex.getMessage());
            return;
        }

        final boolean isForwardedRequest = req.getHeader(FORWARD_REQUEST_HEADER) != null;

        try {
            handle(req, session, id, replicationFactor, isForwardedRequest);
        } catch (IOException exc) {
            session.sendError(Response.GATEWAY_TIMEOUT, GATEWAY_TIMEOUT_ERROR_LOG);
        }
    }

    private void handle(
            final Request request,
            final HttpSession session,
            final String id,
            final ReplicationFactor replicationFactor,
            final boolean isForwardedRequest
    ) throws IOException {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    session.sendResponse(
                            handler.multipleGet(
                                    id,
                                    replicationFactor,
                                    isForwardedRequest
                            )
                    );
                    break;
                case Request.METHOD_PUT:
                    session.sendResponse(handler.multipleUpsert(
                            id,
                            request.getBody(),
                            replicationFactor.getAck(),
                            isForwardedRequest)
                    );
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(
                            handler.multipleDelete(
                                    id,
                                    replicationFactor.getAck(),
                                    isForwardedRequest
                            )
                    );
                    break;
                default:
                    session.sendError(
                            Response.METHOD_NOT_ALLOWED, MESSAGE_MAP.get(ErrorNames.NOT_ALLOWED_METHOD_ERROR)
                    );
                    break;
            }
        } catch (NotEnoughNodesException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.NOT_ENOUGH_NODES), e);
        }
    }

    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Couldn't terminate executor");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : nodesToClients.values()) {
            client.close();
        }
    }

    private static HttpServerConfig getConfig(final int port) {
        if (port <= 1024 || port >= 65536) {
            throw new IllegalArgumentException("Invalid port");
        }
        final AcceptorConfig acc = new AcceptorConfig();
        final HttpServerConfig config = new HttpServerConfig();
        acc.port = port;
        acc.deferAccept = true;
        acc.reusePort = true;
        config.acceptors = new AcceptorConfig[]{acc};
        return config;
    }
}
