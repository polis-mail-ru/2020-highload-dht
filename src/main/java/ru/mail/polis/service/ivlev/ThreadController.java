package ru.mail.polis.service.ivlev;

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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Topology;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.ivlev.util.ConflictUtils;
import ru.mail.polis.service.ivlev.util.SendResponsesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadController extends HttpServer implements Service {

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> clients;
    private final int confirmDefault;
    private final int fromDefault;

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadController.class);
    private static final String RESPONSE_ERROR_LOG_MESSAGE = "Fail send response: ";
    private static final String IS_PROXY_FLAG = "isProxy";

    /**
     * Implementation {@link Service}.
     *
     * @param config       - config
     * @param dao          - dao
     * @param workersCount - count of thread workers
     * @param queueSize    - Blocking queue max size
     */
    public ThreadController(
            final HttpServerConfig config,
            @NotNull final DAO dao,
            final int workersCount,
            final int queueSize,
            @NotNull final Topology<String> topology) throws IOException {
        super(config, dao);
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        this.topology = topology;
        this.fromDefault = topology.getSize();
        this.confirmDefault = fromDefault / 2 + 1;
        this.clients = new HashMap<>();
        for (final String node : topology.getAllNodes()) {
            LOGGER.debug(node);
            if (topology.equalsUrl(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (clients.put(node, client) != null) {
                throw new IllegalArgumentException("Duplicate node!");
            }
            LOGGER.debug("NODE {}, CLIENT {}", node, client.name());
        }
        executor = new ThreadPoolExecutor(
                workersCount, queueSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * End - point status.
     *
     * @param session - http session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, e);
        }
    }

    /**
     * Абстрактный контроллер на get/put/delete.
     *
     * @param id       - id
     * @param replicas - replicas
     * @param session  - session
     * @param request  - request
     */
    @Path("/v0/entity")
    public void sendResponse(
            @Param(value = "id", required = true) final String id,
            @Param("replicas") final String replicas,
            final HttpSession session,
            final Request request) {
        executor.execute(() -> {
            if (id.isEmpty()) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ex) {
                    LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE);
                }
                return;
            }
            if (request.getHeader(IS_PROXY_FLAG) == null) {
                sendReplicationResponse(id, replicas, session, request);
            } else {
                SendResponsesUtils.sendProxyResponse(id, session, request, dao);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, e);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clients.values()) {
            client.close();
        }
    }

    @NotNull
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) {
        try {
            return clients.get(node).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException ex) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Send the resulting response after polling all nodes.
     *
     * @param id       - key
     * @param replicas - ack/from
     * @param session  - session
     * @param request  - request
     */
    public void sendReplicationResponse(
            final String id,
            final String replicas,
            final HttpSession session,
            final Request request) {
        request.addHeader(IS_PROXY_FLAG);
        final int confirm;
        final int from;
        if (replicas == null) {
            confirm = this.confirmDefault;
            from = this.fromDefault;
        } else {
            final ReplicaService replica = ReplicaService.of(replicas);
            confirm = replica.getConfirm();
            from = replica.getFrom();
            if (confirm > from || confirm <= 0) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ex) {
                    LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, ex);
                }
                return;
            }
        }
        final String[] nodes = topology.getNodeByKey(id, from);
        try {
            final List<Response> responses;
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    responses = createResponses(nodes, SendResponsesUtils.get(id, dao), request);
                    session.sendResponse(ConflictUtils.get(responses, confirm));
                    break;
                case Request.METHOD_PUT:
                    responses = createResponses(nodes, SendResponsesUtils.put(id, request, dao), request);
                    session.sendResponse(ConflictUtils.put(responses, confirm));
                    break;
                case Request.METHOD_DELETE:
                    responses = createResponses(nodes, SendResponsesUtils.delete(id, dao), request);
                    session.sendResponse(ConflictUtils.delete(responses, confirm));
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, ex);
        }
    }

    private List<Response> createResponses(
            final String[] nodes,
            final Response nodeResponse,
            final Request request) {
        final List<Response> responses = new ArrayList<>();
        for (final String node : nodes) {
            if (topology.equalsUrl(node)) {
                responses.add(nodeResponse);
            } else {
                responses.add(proxy(node, request));
            }
        }
        return responses;
    }
}
