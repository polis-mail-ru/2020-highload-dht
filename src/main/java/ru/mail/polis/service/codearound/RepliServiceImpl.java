package ru.mail.polis.service.codearound;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class RepliServiceImpl extends HttpServer implements Service {

    private static final String FORWARD_REQUEST_HEADER = "X-OK-Proxy: True";
    private static final Logger LOGGER = LoggerFactory.getLogger(RepliServiceImpl.class);
    private static final String COMMON_RESPONSE_ERROR_LOG = "Error sending response while async handler running";
    private static final String CASE_FORWARDING_ERROR_LOG = "Error forwarding request via proxy";
    private static final String NORMAL_REQUEST_HEADER = "/v0/entity?id=";
    private static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    private static final String IO_ERROR_LOG = "IO exception raised";
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    private static final String REJECT_METHOD_ERROR_LOG = "No match handler exists for request method. "
            + "Failed determining response";
    private static final String GATEWAY_TIMEOUT_ERROR_LOG = "Sending response takes too long. "
            + "Request failed as gateway closed past timeout";

    private final DAO dao;
    private final ExecutorService exec;
    private final Topology<String> topology;
    private final Map<String, HttpClient> nodesToClients;
    private final ReplicationFactor repliFactor;
    private final RepliServiceLsm lsm;

    /**
     * replication-supporting service impl const.
     *
     * @param port request listening and entrance port
     * @param dao DAO instance
     * @param workerPoolSize selector pool size
     * @param queueSize blocking queue capacity
     * @param topology topology implementation instance
     * @param timeout connection timeout
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

        this.dao = dao;
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
        this.nodesToClients = new HashMap<>();
        lsm = new RepliServiceLsm();
        this.repliFactor = new ReplicationFactor(topology.getClusterSize() / 2 + 1, topology.getClusterSize());

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

    /**
     * handles formation request to inform client the server is alive and ready to exchange.
     *
     * @param session ongoing session instance
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
        session.sendResponse(Response.ok("Server is running..."));
    }

    /**
     * resolves request handling by HTTP REST methods, provides any client with response (incl. server outcome code).
     *
     * @param id String object to be processed as a key in terms of data storage design
     * @param req HTTP request
     * @param session ongoing session instance
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

        final ReplicationFactor repliFactorObj = ReplicationFactor
                .getRepliFactor(req.getParameter("replicas"), session, repliFactor);
        final ByteBuffer bufKey = DAOByteOnlyConverter.tuneArrayToBuf(id.getBytes(StandardCharsets.UTF_8));

        if (topology.getClusterSize() > 1) {
            try {
                switch (req.getMethod()) {
                    case Request.METHOD_GET:
                        session.sendResponse(getWithMultipleNodes(id, repliFactorObj, isForwardedRequest));
                        break;
                    case Request.METHOD_PUT:
                        session.sendResponse(upsertWithMultipleNodes(
                                id,
                                req.getBody(),
                                repliFactorObj.getAckValue(),
                                isForwardedRequest));
                        break;
                    case Request.METHOD_DELETE:
                        session.sendResponse(deleteWithMultipleNodes(
                                id,
                                repliFactorObj.getAckValue(),
                                isForwardedRequest));
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
                    runAsyncHandler(session, () -> getWithOnlyNode(bufKey, req));
                    break;
                case Request.METHOD_PUT:
                    runAsyncHandler(session, () -> upsertWithOnlyNode(bufKey, req.getBody(), req));
                    break;
                case Request.METHOD_DELETE:
                    runAsyncHandler(session, () -> deleteWithOnlyNode(bufKey, req));
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, REJECT_METHOD_ERROR_LOG);
                    break;
            }
        }
    }

    /**
     * GET handler applicable for single node topology.
     *
     * @param key - key searched
     * @param req - HTTP request
     * @return HTTP response
     */
    Response getWithOnlyNode(@NotNull final ByteBuffer key, @NotNull final Request req) {

        final String owner = topology.primaryFor(key);
        ByteBuffer buf;
        if (topology.isThisNode(owner)) {
            try {
                buf = dao.get(key);
                return new Response(Response.ok(DAOByteOnlyConverter.readByteArray(buf)));
            } catch (NoSuchElementException exc) {
                LOGGER.info(NOT_FOUND_ERROR_LOG);
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                return new Response(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (IOException exc) {
                LOGGER.error(IO_ERROR_LOG);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * GET handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param repliFactor - replication factor
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response getWithMultipleNodes(
            final String id,
            @NotNull final ReplicationFactor repliFactor,
            final boolean isForwardedRequest) throws IOException {

        int replCounter = 0;
        final String[] nodes = RepliServiceUtils.getNodeReplica(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor,
                isForwardedRequest,
                topology);
        final List<Value> values = new ArrayList<>();
        for (final String node : nodes) {
            try {
                Response response;
                if (topology.isThisNode(node)) {
                    response = RepliServiceUtils.issueInternalResponse(
                            ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)),
                            dao);
                } else {
                    response = nodesToClients.get(node).get(NORMAL_REQUEST_HEADER + id, FORWARD_REQUEST_HEADER);
                }
                if (response.getStatus() == 404 && response.getBody().length == 0) {
                    values.add(Value.resolveMissingValue());
                } else if (response.getStatus() == 500) {
                    continue;
                } else {
                    values.add(Value.getValueFromBytes(response.getBody()));
                }
                replCounter++;
            } catch (HttpException | PoolException | InterruptedException exc) {
                LOGGER.error("Error running GET handler on cluster replica node", exc);
            }
        }
        if (isForwardedRequest || replCounter >= repliFactor.getAckValue()) {
            return RepliServiceUtils.issueExternalResponse(values, nodes, isForwardedRequest);
        } else {
            LOGGER.error(GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * PUT handler impl for single node topology.
     *
     * @param key - target key
     * @param byteVal byte array processed as a key-bound value
     * @param req - HTTP request
     * @return HTTP response
     */
    Response upsertWithOnlyNode(
            @NotNull final ByteBuffer key,
            final byte[] byteVal,
            @NotNull final Request req) {

        final String owner = topology.primaryFor(key);
        final ByteBuffer val = ByteBuffer.wrap(byteVal);

        if (topology.isThisNode(owner)) {
            try {
                dao.upsert(key, val);
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
            } catch (IOException exc) {
                LOGGER.error(IO_ERROR_LOG);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * PUT handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param value byte array processed as a key-bound value
     * @param ackValue - replication quorum factor ('ack' parameter)
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response upsertWithMultipleNodes(
            final String id,
            final byte[] value,
            final int ackValue,
            final boolean isForwardedRequest) throws IOException {

        if (isForwardedRequest) {
            try {
                dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException exc) {
                return new Response(Response.INTERNAL_ERROR, exc.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final String[] nodes = topology.replicasFor(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isThisNode(node)) {
                    dao.upsertValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())), ByteBuffer.wrap(value));
                    ack++;
                } else {
                    final Response response = nodesToClients.get(node)
                            .put(NORMAL_REQUEST_HEADER + id, value, FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 201) {
                        ack++;
                    }
                }
            } catch (IOException | PoolException | InterruptedException | HttpException exc) {
                LOGGER.error("Error running PUT handler on cluster replica node", exc);
            }
        }

        if (ack >= ackValue) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            LOGGER.error(GATEWAY_TIMEOUT_ERROR_LOG);
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * DELETE handler impl for simgle node topology.
     *
     * @param key - target key
     * @param req HTTP request
     * @return HTTP response
     */
    Response deleteWithOnlyNode(
            @NotNull final ByteBuffer key,
            @NotNull final Request req) throws IOException {

        final String owner = topology.primaryFor(key);
        if (topology.isThisNode(owner)) {
            try {
                dao.remove(key);
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (RejectedExecutionException exc) {
                LOGGER.error(QUEUE_LIMIT_ERROR_LOG);
                return new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY);
            } catch (IOException exc) {
                LOGGER.error(IO_ERROR_LOG);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } else {
            return proxy(owner, req);
        }
    }

    /**
     * PUT handler applicable for multi-node topology.
     *
     * @param id - key searched
     * @param ackValue - replication quorum factor ('ack' parameter)
     * @param isForwardedRequest - true if incoming request header indicates
     *                           invocation of proxy-providing method on a previous node
     * @return HTTP response
     */
    Response deleteWithMultipleNodes(
            final String id,
            final int ackValue,
            final boolean isForwardedRequest) throws IOException {

        if (isForwardedRequest) {
            try {
                dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, e.toString().getBytes(Charset.defaultCharset()));
            }
        }
        final String[] nodes = topology.replicasFor(
                ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())),
                repliFactor.getFromValue());
        int ack = 0;
        for (final String node : nodes) {
            try {
                if (topology.isThisNode(node)) {
                    dao.removeValue(ByteBuffer.wrap(id.getBytes(Charset.defaultCharset())));
                    ack++;
                } else {
                    final Response response = nodesToClients.get(node).delete(NORMAL_REQUEST_HEADER + id, FORWARD_REQUEST_HEADER);
                    if (response.getStatus() == 202) {
                        ack++;
                    }
                }
                if (ack == ackValue) {
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                }
            } catch (IOException | PoolException | HttpException | InterruptedException exc) {
                LOGGER.error("Error running DELETE handler on cluster replica node", exc);
            }
        }
        LOGGER.error(GATEWAY_TIMEOUT_ERROR_LOG);
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * handler determined to run by default.
     *
     * @param req client host request
     * @param session ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
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
            }
        });
    }

    /**
     * implements request proxying in case of mismatching current receiver ID (self ID) and target one.
     *
     * @param nodeId request forwarding node ID
     * @param req HTTP request
     */
    private Response proxy(@NotNull final String nodeId, @NotNull final Request req) {
        try {
            req.addHeader("X-Proxy-For: " + nodeId);
            return nodesToClients.get(nodeId).invoke(req);
        } catch (IOException | InterruptedException | HttpException | PoolException exc) {
            LOGGER.error(CASE_FORWARDING_ERROR_LOG, exc);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
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
            LOGGER.error("Failed executor termination");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : nodesToClients.values()) {
            client.close();
        }
    }
}
