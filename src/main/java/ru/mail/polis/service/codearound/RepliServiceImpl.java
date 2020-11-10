package ru.mail.polis.service.codearound;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RepliServiceImpl extends HttpServer implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepliServiceImpl.class);
    private static final String COMMON_RESPONSE_ERROR_LOG = "Error sending response while async handler running";
    private static final String GATEWAY_TIMEOUT_ERROR_LOG = "Sending response takes too long. "
            + "Request failed as gateway closed past timeout";
    private static final String REJECT_METHOD_ERROR_LOG = "No match handler exists for request method. "
            + "Failed determining response";
    public static final String IO_ERROR_LOG = "IO exception raised";
    public static final String FORWARD_REQUEST_HEADER = "PROXY_HEADER";
    @NotNull
    private final ExecutorService exec;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final ReplicationLsm lsm;
    @NotNull
    private final ReplicationFactor repliFactor;

    /**
     * class const.
     *
     * @param port - request listening and entrance port
     * @param dao - DAO instance
     * @param workerPoolSize - selector pool size
     * @param queueSize - blocking queue capacity
     * @param topology - topology implementation instance
     * @param timeout - connection timeout
     */
    public RepliServiceImpl(final int port,
                            @NotNull final DAO dao,
                            final int workerPoolSize,
                            final int queueSize,
                            @NotNull final Topology<String> topology,
                            final int timeout) throws IOException {
        super(TaskServerConfig.getConfig(port));
        assert workerPoolSize > 0;
        assert queueSize > 0;
        this.exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.topology = topology;
        final Map<String, HttpClient> nodesToClients = new HashMap<>();
        repliFactor = new ReplicationFactor(
                topology.getClusterSize() / 2 + 1,
                topology.getClusterSize());
        this.lsm = new ReplicationLsm(dao, topology, nodesToClients, exec);

        for (final String node : topology.getNodes()) {
            if (topology.isThisNode(node)) {
                continue;
            }
            final HttpClient client = HttpClient.newBuilder()
                    .executor(exec)
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            if (nodesToClients.put(node, client) != null) {
                throw new IllegalStateException("Multiple nodes found by same ID");
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

    /**
     * resolves request handling by HTTP REST methods, provides any client with response (incl. server outcome code).
     *
     * @param id - String object to be processed as a key in terms of data storage design
     * @param req - HTTP request
     * @param session - ongoing session instance
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @NotNull final Request req,
                       @NotNull final HttpSession session) throws IOException {

        if (id.isEmpty()) {
            session.sendError(Response.BAD_REQUEST,"Identifier is required as parameter. Error handling request");
        }
        if (topology.getNodes().size() > 1) {
            invokeHandlerByMethod(id, req, session);
        } else {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            executeAsync(req, key, session);
        }
    }

    /**
     * coordinator for method-specific handlers to manage consistent processing, response as well.
     *
     * @param id - target key (as String-defined data)
     * @param req - HTTP request
     * @param session - ongoing HTTP session instance
     */
    public void invokeHandlerByMethod(@NotNull final String id,
                                      @NotNull final Request req,
                                      @NotNull final HttpSession session) throws IOException {
        final String[] nodes;
        final ReplicationFactor repliFactorObj = ReplicationFactor
                .defaultRepliFactor(req.getParameter("replicas"), repliFactor, session);
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (req.getHeader(FORWARD_REQUEST_HEADER) != null) {
            nodes = new String[]{ topology.getThisNode() };
        } else {
            nodes = topology.replicasFor(key, repliFactorObj.getFromValue());
        }

        try {
            switch (req.getMethod()) {
                case Request.METHOD_GET:
                    lsm.getWithMultipleNodes(key, nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                case Request.METHOD_PUT:
                    lsm.upsertWithMultipleNodes(key, nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                case Request.METHOD_DELETE:
                    lsm.deleteWithMultipleNodes(key, nodes, req, repliFactorObj.getAckValue(), session);
                    return;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, RepliServiceImpl.REJECT_METHOD_ERROR_LOG);
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, GATEWAY_TIMEOUT_ERROR_LOG);
        }
    }

    /**
     * resolves async request processing in single node cluster.
     *
     * @param req - HTTP request
     * @param key - String object to be processed as a key in terms of data storage design
     * @param session - ongoing session instance
     */
    private void executeAsync(@NotNull final Request req,
                              @NotNull final ByteBuffer key,
                              @NotNull final HttpSession session) throws IOException {
        switch (req.getMethod()) {
            case Request.METHOD_GET:
                runAsyncHandler(session, () -> lsm.getWithOnlyNode(key));
                break;
            case Request.METHOD_PUT:
                runAsyncHandler(session, () -> lsm.upsertWithOnlyNode(key, req.getBody()));
                break;
            case Request.METHOD_DELETE:
                runAsyncHandler(session, () -> lsm.deleteWithOnlyNode(key));
                break;
            default:
                session.sendError(Response.METHOD_NOT_ALLOWED, REJECT_METHOD_ERROR_LOG);
                break;
        }
    }

    /**
     * handler determined to run by default.
     *
     * @param req - HTTP request
     * @param session - ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * switches request handling to async-featured process.
     *
     * @param session ongoing HTTP session
     * @param async interface design object to enable async handler execution
     */
    private void runAsyncHandler(@NotNull final HttpSession session, final AsyncExec async) {
        exec.execute(() -> {
            try {
                session.sendResponse(async.exec());
            } catch (IOException exc) {
                LOGGER.error(COMMON_RESPONSE_ERROR_LOG);
                try {
                    session.sendError(Response.INTERNAL_ERROR, COMMON_RESPONSE_ERROR_LOG);
                } catch (IOException e) {
                    LOGGER.error(IO_ERROR_LOG);
                }
            }
        });
    }
}
