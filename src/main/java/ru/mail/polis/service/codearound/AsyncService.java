package ru.mail.polis.service.codearound;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
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

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService exec;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodeToClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);
    private static final String NOT_FOUND_ERROR_LOG = "Match key is missing, no value can be retrieved";
    private static final String IO_ERROR_LOG = "IO exception raised";
    private static final String QUEUE_LIMIT_ERROR_LOG = "Queue is full, lacks free capacity";
    private static final String COMMON_RESPONSE_ERROR_LOG = "Error sending response while async handler running";
    private static final String CASE_FORWARDING_ERROR_LOG = "Error forwarding request via proxy";

    /**
     * async service impl const.
     *
     * @param port local server listening port
     * @param dao DAO instance
     * @param workerPoolSize selector pool size
     */
    public AsyncService(final int port,
                        @NotNull final DAO dao,
                        final int workerPoolSize,
                        final int queueSize,
                        @NotNull final Topology<String> topology) throws IOException {

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
        this.nodeToClient = new HashMap<>();

        for (final String node : topology.getNodes()) {
            if (topology.isSelfId(node)) {
                continue;
            }

            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));

            if(nodeToClient.put(node, client) != null) {
                throw new IllegalStateException("Multiple nodes found by same ID");
            }
        }
    }

    /**
     * fires formation request to make sure server is alive.
     *
     * @param session ongoing session instance
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
           session.sendResponse(Response.ok("Server is running..."));
    }

    /**
     * returns server status, request-specific response as well.
     *
     * @param id String object to be processed as a key in terms of data storage design
     * @param req HTTP request
     * @param session ongoing session instance
     */
    @Path("/v0/entity")
    public void entity(
            @Param(value = "id", required = true) final String id,
            @NotNull final Request req,
            @NotNull final HttpSession session) throws NoSuchMethodException, IOException {

        if (id.isEmpty()) {
            session.sendError(Response.BAD_REQUEST,"Identifier is required as parameter. Error handling request");
            return;
        }

        final ByteBuffer buf = DAOByteOnlyConverter.tuneArrayToBuf(id.getBytes(StandardCharsets.UTF_8));

        switch (req.getMethod()) {
            case Request.METHOD_GET:
                runAsyncHandler(session, () -> get(buf, req));
                break;
            case Request.METHOD_PUT:
                runAsyncHandler(session, () -> upsert(buf, req.getBody(), req));
                break;
            case Request.METHOD_DELETE:
                runAsyncHandler(session, () -> delete(buf, req));
                break;
            default:
                throw new NoSuchMethodException("No handler is available for request method. "
                        + "Failed determining response");
        }
    }

    /**
     * GET handler impl.
     *
     * @param key - key searched
     * @param req - HTTP request
     * @return HTTP response
     */
    private Response get(@NotNull final ByteBuffer key,
                         @NotNull final Request req) {

        final String owner = topology.primaryFor(key);
        ByteBuffer buf;
        if (topology.isSelfId(owner)) {
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
     * PUT handler impl.
     *
     * @param key - target key
     * @param byteVal byte array processed as a key-bound value
     * @param req - HTTP request
     * @return HTTP response
     */
    private Response upsert(@NotNull final ByteBuffer key,
                             final byte[] byteVal,
                             @NotNull final Request req) {

        final String owner = topology.primaryFor(key);
        final ByteBuffer val = ByteBuffer.wrap(byteVal);

        if (topology.isSelfId(owner)) {
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
     * DELETE handler impl.
     *
     * @param key - target key
     * @param req HTTP request
     * @return HTTP response
     */
    private Response delete(@NotNull final ByteBuffer key,
                            @NotNull final Request req) {

        final String owner = topology.primaryFor(key);
        if (topology.isSelfId(owner)) {
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
                LOGGER.error(IO_ERROR_LOG);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException exc2) {
                    LOGGER.error(COMMON_RESPONSE_ERROR_LOG);
                }
            }
        });
    }

    /**
     * implements request proxying in case of mismatching current receiver ID (self ID) and target one.
     *
     * @param nodeId request forwarding node ID
     * @param req HTTP request
     */
    private Response proxy(@NotNull final String nodeId,
                           @NotNull final Request req) {
        try {
            req.addHeader("X-Proxy-For: " + nodeId);
            return nodeToClient.get(nodeId).invoke(req);
        } catch (IOException | InterruptedException | HttpException | PoolException exc) {
            LOGGER.error(CASE_FORWARDING_ERROR_LOG, exc);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * default handler.
     * @param req client host request
     * @param session ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
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
    }
}
