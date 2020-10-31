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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MoribundService extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    private final int ackDefault;
    private final int fromDefault;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> clients;
    private final String PROXY_HEADER = "PROXY";
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
                           final int timeout,
                           @NotNull final Topology<String> topology) throws IOException {
        super(getConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        this.topology = topology;
        this.fromDefault = topology.size();
        this.ackDefault = fromDefault / 2 + 1;
        this.clients = new HashMap<>();
        for (final String node : topology.allNodes()) {
            if (topology.isMe(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=" + timeout));
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
            if (id.isEmpty()) {
                logger.warn("Id is empty!");
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Can't send response!");
                }
                return;
            }
            if (request.getHeader(PROXY_HEADER) == null) {
                sendReplicationResponse(id, replicas, session, request);
            } else {
                sendProxyResponse(id, session, request);
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
    public void sendProxyResponse(final String id, final HttpSession session, final Request request) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    session.sendResponse(DAOServiceMethods.get(id, dao));
                    break;
                case Request.METHOD_PUT:
                    session.sendResponse(DAOServiceMethods.put(id, request, dao));
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(DAOServiceMethods.delete(id, dao));
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
            final Replica replica = Replica.of(replicas);
            ack = replica.getAck();
            from = replica.getFrom();
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
                    responses = getAllResponses(nodes, DAOServiceMethods.get(id, dao), request);
                    session.sendResponse(Consensus.get(responses, ack));
                    break;
                case Request.METHOD_PUT:
                    responses = getAllResponses(nodes, DAOServiceMethods.put(id, request, dao), request);
                    session.sendResponse(Consensus.put(responses, ack));
                    break;
                case Request.METHOD_DELETE:
                    responses = getAllResponses(nodes, DAOServiceMethods.delete(id, dao), request);
                    session.sendResponse(Consensus.delete(responses, ack));
                    break;
                default:
                    break;
            }
        } catch (
            IOException ioException) {
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
