package ru.mail.polis.service.codearound;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RepliServiceImpl extends HttpServer implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepliServiceImpl.class);
    private static final String COMMON_RESPONSE_ERROR_LOG = "Error sending response while async handler running";
    private static final String REJECT_METHOD_ERROR_LOG = "No match handler exists for request method. "
                 + "Failed determining response";
    public static final String IO_ERROR_LOG = "IO exception raised";
    public static final String FORWARD_REQUEST_HEADER = "X-OK-Proxy: True";
    public static final String GATEWAY_TIMEOUT_ERROR_LOG = "Sending response takes too long. "
            + "Request failed as gateway closed past timeout";
    @NotNull
    private final ExecutorService exec;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodesToClients;
    @NotNull
    private final ReplicationFactor repliFactor;
    @NotNull
    private final ReplicationLsm lsm;
    @NotNull
    private final DAO dao;

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
                        //.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.topology = topology;
        this.dao = dao;
        this.nodesToClients = new HashMap<>();
        this.repliFactor = new ReplicationFactor(topology.getClusterSize() / 2 + 1, topology.getClusterSize());
        this.lsm = new ReplicationLsm(dao, topology, nodesToClients, repliFactor);
        for (final String node : topology.getNodes()) {
            if (topology.isThisNode(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=" + timeout));
            if (nodesToClients.put(node, client) != null) {
                throw new IllegalStateException("Multiple nodes found by same ID");
            }
        }
    }

    @Override
    public HttpSession createSession(@NotNull final Socket socket) {
        return new ChunkStreamingSession(socket, this);
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
     * resolves request handling by HTTP REST methods, provides any client with response (incl. server return code).
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
            return;
        }

        boolean isForwardedRequest = false;
        if (req.getHeader(FORWARD_REQUEST_HEADER) != null) {
            isForwardedRequest = true;
        }

        final boolean forwardedStatus = isForwardedRequest;
        final ReplicationFactor repliFactorObj =
                ReplicationFactor.defaultRepliFactor(req.getParameter("replicas"), repliFactor, session);
        final ByteBuffer bufKey = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        if (topology.getClusterSize() > 1) {
            try {
                switch (req.getMethod()) {
                    case Request.METHOD_GET:
                        runAsyncHandler(session, () -> lsm.getWithMultipleNodes(
                                id,
                                repliFactorObj,
                                forwardedStatus));
                        break;
                    case Request.METHOD_PUT:
                        runAsyncHandler(session, () -> lsm.upsertWithMultipleNodes(
                                id,
                                req.getBody(),
                                repliFactorObj.getAckValue(),
                                forwardedStatus));
                        break;
                    case Request.METHOD_DELETE:
                        runAsyncHandler(session, () -> lsm.deleteWithMultipleNodes(
                                id,
                                repliFactorObj.getAckValue(),
                                forwardedStatus));
                        break;
                    default:
                        session.sendError(Response.METHOD_NOT_ALLOWED, REJECT_METHOD_ERROR_LOG);
                        break;
                }
            } catch (IOException exc) {
                session.sendError(Response.GATEWAY_TIMEOUT, GATEWAY_TIMEOUT_ERROR_LOG);
            }
        } else {
            switch (req.getMethod()) {
                case Request.METHOD_GET:
                    runAsyncHandler(session, () -> lsm.getWithOnlyNode(bufKey, req));
                    break;
                case Request.METHOD_PUT:
                    runAsyncHandler(session, () -> lsm.upsertWithOnlyNode(bufKey, req.getBody(), req));
                    break;
                case Request.METHOD_DELETE:
                    runAsyncHandler(session, () -> lsm.deleteWithOnlyNode(bufKey, req));
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, REJECT_METHOD_ERROR_LOG);
                    break;
            }
        }
    }

    /**
     * handler executable for range requests only.
     *
     * @param req - client host request
     * @param session - ongoing session instance
     */
    @Path("/v0/entities")
    public void handleRangeRequest(@NotNull final Request req,
                                   @NotNull final HttpSession session) throws IOException {

        final String start = req.getParameter("start=");

        if (start == null || start.isEmpty()) {
            session.sendError(Response.BAD_REQUEST, "'start' parameter is missing or present without any data");
            return;
        }

        if (req.getMethod() != Request.METHOD_GET) {
            session.sendError(
                    Response.METHOD_NOT_ALLOWED,
                    "Inconsistent method selection for sending range request (GET is only supported)");
            return;
        }

        final String end = req.getParameter("end=");
        final ByteBuffer endBuf = end == null ? null : ByteBuffer.wrap(end.getBytes(Charset.defaultCharset()));

        final ByteBuffer startBuf = ByteBuffer.wrap(start.getBytes(Charset.defaultCharset()));
        final Iterator<Record> records = dao.range(
                startBuf,
                endBuf);
        ((ChunkStreamingSession) session).initStreaming(records);
    }

    /**
     * handler determined to run by default.
     *
     * @param req - client host request
     * @param session - ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * switches request handling to async-featured process.
     *
     * @param session ongoing HTTP session
     * @param async interface design object to run async processing
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
                    System.out.println(IO_ERROR_LOG);
                }
            }
        });
    }

    /**
     * terminates ExecutorService process.
     */
    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //LOGGER.error("Failed executor termination");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : nodesToClients.values()) {
            client.close();
        }
    }
}
