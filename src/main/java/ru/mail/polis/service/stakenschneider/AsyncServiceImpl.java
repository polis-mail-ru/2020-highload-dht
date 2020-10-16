package ru.mail.polis.service.stakenschneider;

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
import one.nio.net.Socket;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.NoSuchElementLiteException;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);

    @NotNull
    private final DAO dao;
    @NotNull
    private final Executor executor;
    private final Nodes nodes;
    private final Map<String, HttpClient> clusterClients;

    /**
     * Simple Async HTTP server.
     *
     * @param config         - config
     * @param dao            - storage interface
     * @param nodes          - topology
     * @param clusterClients - clusterClients
     * @throws IOException - exception
     */
    private AsyncServiceImpl(final HttpServerConfig config,
                             @NotNull final DAO dao,
                             @NotNull final Nodes nodes,
                             @NotNull final Map<String, HttpClient> clusterClients) throws IOException {
        super(config);
        this.dao = dao;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("worker").build());
        this.nodes = nodes;
        this.clusterClients = clusterClients;
    }

    /**
     * Create Async HTTP server.
     */
    public static Service create(final int port,
                                 @NotNull final DAO dao,
                                 @NotNull final Nodes nodes) throws IOException {
        final var acceptor = new AcceptorConfig();
        final var config = new HttpServerConfig();
        acceptor.port = port;
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.maxWorkers = Runtime.getRuntime().availableProcessors();
        config.queueTime = 10;
        final Map<String, HttpClient> clusterClients = new HashMap<>();
        for (final String it : nodes.getNodes()) {
            if (!nodes.getId().equals(it) && !clusterClients.containsKey(it)) {
                clusterClients.put(it, new HttpClient(new ConnectionString(it + "?timeout=100")));
            }
        }
        return new AsyncServiceImpl(config, dao, nodes, clusterClients);
    }

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StorageSession(socket, this);
    }

    private void executeAsync(@NotNull final HttpSession session,
                              @NotNull final Action action) {
        executor.execute(() -> {
            try {
                session.sendResponse(action.act());
            } catch (IOException e) {
                try {
                    session.sendError(Response.INTERNAL_ERROR, e.getMessage());
                } catch (IOException ex) {
                    log.info("something has gone terribly wrong", ex);
                }
            }
        });
    }

    private Response forwardRequestTo(@NotNull final String cluster,
                                      final Request request) throws IOException {
        try {
            return clusterClients.get(cluster).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            throw new IOException("fail", e);
        }
    }

    @FunctionalInterface
    interface Action {
        Response act() throws IOException;
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        final var response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        log.warn("Can't find handler for {}", request.getPath());
        session.sendResponse(response);
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
     * Access to DAO.
     *
     * @param id      - key of entity
     * @param request - requests: GET, PUT, DELETE
     * @param session - HttpSession
     */
    @Path("/v0/entity")
    public void entity(@Param("id") final String id,
                       @NotNull final Request request,
                       final HttpSession session) throws IOException {
        if (id == null || id.isEmpty()) {
            try {
                log.info("id is null or empty");
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                log.error("something has gone terribly wrong", e);
                throw new UncheckedIOException(e);
            }
            return;
        }

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final String keyClusterPartition = nodes.primaryFor(key);

        if (!nodes.getId().equals(keyClusterPartition)) {
            executeAsync(session, () -> forwardRequestTo(keyClusterPartition, request));
            return;
        }

        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    executeAsync(session, () -> get(key));
                    break;
                case Request.METHOD_PUT:
                    executeAsync(session, () -> put(key, request));
                    break;
                case Request.METHOD_DELETE:
                    executeAsync(session, () -> delete(key));
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
                    break;
            }
        } catch (IOException e) {
            log.error("Internal error", e);
            session.sendError(Response.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Extract entities from start to end.
     *
     * @param start   - start key
     * @param end     - end key (optional)
     * @param request - request
     * @param session - HttpSession
     */
    @Path("/v0/entities")
    public void entities(@Param("start") final String start,
                         @Param("end") String end,
                         @NotNull final Request request,
                         @NotNull final HttpSession session) throws IOException {
        if (request.getMethod() != Request.METHOD_GET) {
            session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
            return;
        }

        if (start == null || start.isEmpty()) {
            session.sendError(Response.BAD_REQUEST, "No start");
            return;
        }

        if (end != null && end.isEmpty()) {
            end = null;
        }

        final Iterator<Record> records =
                dao.range(ByteBuffer.wrap(start.getBytes(StandardCharsets.UTF_8)),
                        end == null ? null : ByteBuffer.wrap(end.getBytes(StandardCharsets.UTF_8)));
        ((StorageSession) session).stream(records);

    }

    private Response get(final ByteBuffer key) {
        try {
            final ByteBuffer value = dao.get(key);
            final ByteBuffer duplicate = value.duplicate();
            final var body = new byte[duplicate.remaining()];
            duplicate.get(body);
            return new Response(Response.OK, body);
        } catch (NoSuchElementLiteException | IOException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private Response put(final ByteBuffer key, final Request request) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(request.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response delete(final ByteBuffer key) throws IOException {
        dao.remove(key);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
