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
import one.nio.net.Socket;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.suhova.Topology;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MoribundService extends HttpServer implements Service {
    private static final String PROXY_HEADER = "PROXY";
    private static final Logger logger = LoggerFactory.getLogger(MoribundService.class);
    @NotNull
    private final DAOServiceMethods daoServiceMethods;
    @NotNull
    private final ExecutorService executor;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> clients;
    private final int ackDefault;
    private final int fromDefault;

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
                           final int timeout,
                           @NotNull final Topology<String> topology) throws IOException {
        super(getConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.daoServiceMethods = new DAOServiceMethods(dao);
        this.topology = topology;
        this.fromDefault = topology.size();
        this.ackDefault = topology.quorumSize();
        this.clients = new HashMap<>();
        for (final String node : topology.allNodes()) {
            if (!topology.isMe(node)) {
                final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=" + timeout));
                if (clients.put(node, client) != null) {
                    throw new IllegalArgumentException("Duplicate node: " + node);
                }
            }
        }
        executor = new ThreadPoolExecutor(
            workersCount, queueSize,
            3L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                .setNameFormat("worker_%d")
                .build(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private boolean badRequestIfEmptyId(final String start, final String end, final HttpSession session) {
        if (start.isEmpty() || ((end != null) && end.isEmpty())) {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException ioException) {
                logger.error("Can't send response!");
            }
            return true;
        }
        return false;
    }

    /**
     * Range get from DAO.
     * Path /v0/entities
     *
     * @param start   - start id
     * @param end     - end id
     * @param session - session
     */
    @Path("/v0/entities")
    public void sendRangeResponse(@Param(value = "start", required = true) final String start,
                                  @Param(value = "end") final String end,
                                  final HttpSession session) {
        if (!badRequestIfEmptyId(start, end, session)) {
            try {
                ((StreamSession) session).setIterator(daoServiceMethods.range(start, end));
            } catch (IOException ioException) {
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Can't send response", e);
                }
            }
        }
    }

    /**
     * Request to delete/put/get in DAO.
     * Path /v0/entity
     *
     * @param id      - key
     * @param session - session
     * @param request - request
     */
    @Path("/v0/entity")
    public void sendResponse(@Param(value = "id", required = true) final String id,
                             @Param("replicas") final String replicas,
                             final HttpSession session,
                             final Request request) {
        executor.execute(() -> {
            if (!badRequestIfEmptyId(id, null, session)) {
                if (request.getHeader(PROXY_HEADER) == null) {
                    sendReplicationResponse(id, replicas, session, request);
                } else {
                    localResponse(id, session, request);
                }
            }
        });
    }

    /**
     * Send a response to a proxied request from another node.
     *
     * @param id      - key
     * @param session - session
     * @param request - request
     */
    public void localResponse(final String id, final HttpSession session, final Request request) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    session.sendResponse(daoServiceMethods.get(id));
                    break;
                case Request.METHOD_PUT:
                    session.sendResponse(daoServiceMethods.put(id, request));
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(daoServiceMethods.delete(id));
                    break;
                default:
                    break;
            }
        } catch (IOException ioException) {
            logger.error("Can't send response.", ioException);
        }
    }

    private List<Response> getAllResponses(final String[] nodes,
                                           final Response currentNodeResponse,
                                           final Request request) {
        final List<Response> responses = new ArrayList<>();
        for (final String node : nodes) {
            if (topology.isMe(node)) {
                responses.add(currentNodeResponse);
            } else {
                responses.add(proxy(node, request));
            }
        }
        return responses;
    }

    /**
     * Send the resulting response after polling all nodes.
     *
     * @param id       - key
     * @param replicas - ack/from
     * @param session  - session
     * @param request  - request
     */
    public void sendReplicationResponse(final String id,
                                        final String replicas,
                                        final HttpSession session,
                                        final Request request) {
        request.addHeader(PROXY_HEADER);
        final int ack;
        final int from;
        if (replicas == null) {
            ack = ackDefault;
            from = fromDefault;
        } else {
            final Replication replication = Replication.of(replicas);
            ack = replication.getAcks();
            from = replication.getReplicas();
            if (ack > from || ack <= 0) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Can't send response about BAD_REQUEST", ioException);
                }
                return;
            }
        }
        final String[] nodes = topology.getNodesByKey(id, from);
        try {
            final List<Response> responses;
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    responses = getAllResponses(nodes, daoServiceMethods.get(id), request);
                    session.sendResponse(Consensus.get(responses, ack));
                    break;
                case Request.METHOD_PUT:
                    responses = getAllResponses(nodes, daoServiceMethods.put(id, request), request);
                    session.sendResponse(Consensus.put(responses, ack));
                    break;
                case Request.METHOD_DELETE:
                    responses = getAllResponses(nodes, daoServiceMethods.delete(id), request);
                    session.sendResponse(Consensus.delete(responses, ack));
                    break;
                default:
                    break;
            }
        } catch (IOException ioException) {
            logger.error("Can't send resulting response.", ioException);
        }
    }

    /**
     * All requests to /v0/status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            logger.error("FAIL STATUS! Can't send response.", e);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            logger.error("Can't send response.", e);
        }
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

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StreamSession(socket, this);
    }
}
