package ru.mail.polis.service.suhova;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.suhova.Topology;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
    private final Map<String, java.net.http.HttpClient> clients;
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
        final Executor clientExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                .setNameFormat("client_%d")
                .build());
        for (final String node : topology.allNodes()) {
            if (topology.isMe(node)) {
                continue;
            }
            final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .executor(clientExecutor)
                .connectTimeout(Duration.ofSeconds(1))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
            if (clients.put(node, client) != null) {
                throw new IllegalArgumentException("Duplicate node: " + node);
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
                localResponse(id, session, request);
            }
        });
    }

    private void executeAsync(@NotNull final HttpSession session,
                              @NotNull final Supplier<Response> method) {

        executor.execute(() -> {
                try {
                    session.sendResponse(method.get());
                } catch (IOException ioException) {
                    logger.error("Can't send response.", ioException);
                }
            }
        );
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

    private Collection<CompletableFuture<Response>> getAllResponses(final String[] nodes,
                                                                    final CompletableFuture<Response> currentNodeResponse,
                                                                    final Request request) {
        final Collection<CompletableFuture<Response>> responses = new ConcurrentLinkedQueue<>();
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
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                executeAsync(session,
                    () -> Consensus.get(getAllResponses(nodes,
                        CompletableFuture.supplyAsync(() -> daoServiceMethods.get(id), executor), request),
                        ack));
                break;
            case Request.METHOD_PUT:
                executeAsync(session,
                    () -> Consensus.get(getAllResponses(nodes,
                        CompletableFuture.supplyAsync(() -> daoServiceMethods.put(id, request), executor), request),
                        ack)
                );
                break;
            case Request.METHOD_DELETE:
                executeAsync(session,
                    () -> Consensus.get(
                        getAllResponses(nodes,
                            CompletableFuture.supplyAsync(() -> daoServiceMethods.delete(id), executor), request),
                        ack)
                );
                break;
            default:
                break;
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
    private CompletableFuture<Response> proxy(@NotNull final String node,
                                              @NotNull final Request request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .method(request.getMethodName(), HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
            .timeout(Duration.ofSeconds(1))
            .uri(URI.create(node + "/v0/entity?id=" + request.getParameter("id")));
        builder.header(PROXY_HEADER, "1");
        final HttpRequest httpRequest = builder.build();
        return clients.get(node).sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
            .thenApplyAsync(a -> {
                final Response response = new Response(status(a.statusCode()), a.body());
                final Map<String, List<String>> headers = a.headers().map();
                for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                    response.addHeader(header.getKey() + header.getValue());
                }
                return response;
            });
    }

    private String status(final int code) {
        switch (code) {
            case 200:
                return Response.OK;
            case 201:
                return Response.CREATED;
            case 202:
                return Response.ACCEPTED;
            case 404:
                return Response.NOT_FOUND;
            case 400:
                return Response.BAD_REQUEST;
            case 502:
                return Response.BAD_GATEWAY;
            case 504:
                return Response.GATEWAY_TIMEOUT;
            case 505:
                return Response.HTTP_VERSION_NOT_SUPPORTED;
            default:
                return Response.INTERNAL_ERROR;
        }
    }
}
