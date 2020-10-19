package ru.mail.polis.service.suhova;

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
import ru.mail.polis.dao.suhova.Topology;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MoribundService extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> clients;
    private static final Logger logger = LoggerFactory.getLogger(MoribundService.class);

    /**
     * Implementation {@link Service}.
     *
     * @param port         - port
     * @param dao          - dao
     * @param workersCount - count of executor workers
     * @param queueSize    - ArrayBlockingQueue max size
     */
    public MoribundService(final int port,
                           @NotNull final DAO dao,
                           final int workersCount,
                           final int queueSize,
                           @NotNull final Topology<String> topology) throws IOException {
        super(getConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        this.topology = topology;
        this.clients = new HashMap<>();
        for (final String node : topology.allNodes()) {
            if (topology.isMe(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (clients.put(node, client) != null) {
                throw new IllegalArgumentException("Duplicate node!");
            }
        }

        executor = new ThreadPoolExecutor(
            workersCount, queueSize,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                .setNameFormat("worker_%d")
                .build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private Response get(final String id) {
        try {
            if (id.isEmpty()) {
                logger.warn("FAIL GET! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            return new Response(Response.OK, toByteArray(dao.get(toByteBuffer(id))));
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            logger.error("FAIL GET! id: {}, error: {}", id, e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }


    private Response put(final String id, final Request request) {
        try {
            if (id.isEmpty()) {
                logger.warn("FAIL PUT! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("FAIL PUT! id: {}, request: {}, error: {}", id, request.getBody(), e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }


    private Response delete(final String id) {
        try {
            if (id.isEmpty()) {
                logger.warn("FAIL DELETE! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.remove(toByteBuffer(id));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("FAIL DELETE! id: {}, error: {}", id, e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Request to delete/put/get in DAO
     * Path /v0/entity
     *
     * @param id      - key
     * @param session - session
     * @param request - request
     */
    @Path("/v0/entity")
    public void sendResponse(@Param(value = "id", required = true) final String id,
                             final HttpSession session,
                             final Request request) {
        executor.execute(() -> {
            final String node = topology.getNodeByKey(id);
            if (topology.isMe(node)) {
                try {
                    switch (request.getMethod()) {
                        case Request.METHOD_GET:
                            session.sendResponse(get(id));
                            break;
                        case Request.METHOD_PUT:
                            session.sendResponse(put(id, request));
                            break;
                        case Request.METHOD_DELETE:
                            session.sendResponse(delete(id));
                            break;
                        default:
                            break;
                    }
                } catch (IOException ioException) {
                    logger.error("Can't send response.", ioException);
                }
            } else {
                try {
                    session.sendResponse(proxy(node, request));
                } catch (IOException ioException) {
                    logger.error("Can't send response by proxy.", ioException);
                }
            }
        });
    }

    /**
     * All requests to /v0/status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        executor.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                logger.error("FAIL STATUS! Can't send response.", e);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        executor.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Can't send response.", e);
            }
        });
    }

    private static HttpServerConfig getConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        acceptorConfig.deferAccept = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    private static ByteBuffer toByteBuffer(@NotNull final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] toByteArray(@NotNull final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown execution");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clients.values()) {
            client.close();
        }
    }

    @NotNull
    private Response proxy(@NotNull final String node,
                           @NotNull final Request request) {
        try {
            return clients.get(node).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException e) {
            logger.error("Can't proxy request! ", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
