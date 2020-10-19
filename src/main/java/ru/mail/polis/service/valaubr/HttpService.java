package ru.mail.polis.service.valaubr;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.valaubr.topology.ModularTopology;
import ru.mail.polis.service.valaubr.topology.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

public class HttpService extends HttpServer implements Service {
    private static final String UNIVERSAL_MESSAGE = "Empty id && response is dropped";
    private final DAO dao;
    private final Logger logger = LoggerFactory.getLogger(HttpService.class);
    private final ExecutorService executor;
    private final Map<String, HttpClient> nodeToClient;
    private final Topology<String> topology;

    /**
     * Constructor of the service.
     *
     * @param port            - port of connection
     * @param base            - object of storage
     * @param modularTopology - topology of service
     * @throws IOException - exceptions
     */
    public HttpService(final int port,
                       @NotNull final DAO base,
                       final int threadPool,
                       final int queueSize,
                       @NotNull final ModularTopology modularTopology) throws IOException {
        super(config(port, threadPool));
        dao = base;
        topology = modularTopology;
        nodeToClient = new HashMap<>();

        for (final String node : topology.all()) {
            if (topology.isMe(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (nodeToClient.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }

        this.executor = new ThreadPoolExecutor(
                threadPool,
                queueSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker-%d")
                        .setUncaughtExceptionHandler((thread, e) -> logger.error("error in {} thread", thread, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );

    }

    private static HttpServerConfig config(final int port, final int threadPool) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        httpServerConfig.selectors = threadPool;
        httpServerConfig.maxWorkers = threadPool;
        httpServerConfig.minWorkers = threadPool;
        return httpServerConfig;
    }

    private byte[] converterFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    /**
     * Return status of server.
     *
     * @return 200 - ok
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    /**
     * Getting Entity by id.
     *
     * @param id - Entity id
     *           200 - ok
     *           400 - Empty id in param
     *           404 - No such element in dao
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(required = true, value = "id") @NotNull final String id,
                    @NotNull final HttpSession session,
                    @NotNull final Request request) {
        if (checkId(id, session)) {
            return;
        }
        executor.execute(() -> {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
            final String node = topology.primaryFor(key);
            if (topology.isMe(node)) {
                try {
                    session.sendResponse(Response.ok(
                            converterFromByteBuffer(dao.get(key))));
                } catch (NoSuchElementException e) {
                    logger.error("Record not exist by id = {}", id);
                    try {
                        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Record not exist && response is dropped: " + ioException);
                    }
                } catch (IOException e) {
                    logger.error("Error when getting record", e);
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error when getting record && response is dropped: ", ioException);
                    }
                }
            } else {
                proxy(node, request, session);
            }
        });
    }

    /**
     * Insertion entity dao by id.
     *
     * @param id - Entity id
     *           201 - Create entity
     *           400 - Empty id in param
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(required = true, value = "id") @NotNull final String id,
                    @NotNull final Request request, @NotNull final HttpSession session) {
        if (checkId(id, session)) {
            return;
        }
        executor.execute(() -> {
            final String node = topology.primaryFor(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
            if (topology.isMe(node)) {
                try {
                    dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
                    session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Error when putting record", e);
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Put error && response is dropped:", ioException);
                    }
                }
            } else {
                proxy(node, request, session);
            }
        });
    }

    /**
     * Deleting entity from dao by id.
     *
     * @param id - Entity id
     *           202 - Delete entity
     *           400 - Empty id in param
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(required = true, value = "id") @NotNull final String id,
                       @NotNull final HttpSession session,
                       @NotNull final Request request) {
        if (checkId(id, session)) {
            return;
        }
        executor.execute(() -> {
            final String node = topology.primaryFor(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
            if (topology.isMe(node)) {
                try {
                    dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
                    session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Error when deleting record", e);
                    try {
                        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    } catch (IOException ioException) {
                        logger.error("Error when deleting record && response is dropped", e);
                    }
                }
            } else {
                proxy(node, request, session);
            }
        });
    }

    private void proxy(@NotNull String node,
                       @NotNull Request request,
                       @NotNull HttpSession session) {
        try {
            session.sendResponse(nodeToClient.get(node).invoke(request));
        } catch (Exception e) {
            logger.error("Can`t proxy request: ", e);
        }
    }

    private boolean checkId(String id, HttpSession session) {
        if (id.strip().isEmpty()) {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                return true;
            } catch (IOException e) {
                logger.error(UNIVERSAL_MESSAGE, e);
                return false;
            }
        }
        return false;
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws
            IOException {
        executor.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("handleDefault can`t send response", e);
            }
        });
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Executor don`t wanna stop!!!", e);
            Thread.currentThread().interrupt();
        }
    }
}