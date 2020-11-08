package ru.mail.polis.service.ivanovandrey;

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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {
    private final BasicFuctions basicFuctions;
    private final ExecutorService execPool;
    private final Set<String> topology;
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    private final Replica defaultReplicas;

    private static final String PROXY_HEADER = "X-OK-Proxy";
    private final SimpleTopology simpleTopology;

    /**
     * Constructor.
     * @param port - service configuration.
     * @param dao - dao implementation.
     * @param executors - number of executors.
     * @param queueSize - size of queue in ThreadPoolExecutor.
     * @param topology - set of cluster nodes.
     */
    public AsyncServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final int executors,
            final int queueSize,
            @NotNull final Set<String> topology) throws IOException {
        super(newConfig(port));
        this.execPool = new ThreadPoolExecutor(
                executors,
                executors,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Executor-%d")
                        .build()
        );
        this.basicFuctions = new BasicFuctions(dao,execPool);
        this.simpleTopology = new SimpleTopology(executors,execPool, port);
        this.topology = topology;
        this.defaultReplicas = new Replica(topology.size());
    }

    private static HttpServerConfig newConfig(final int port) {
        final HttpServerConfig conf = new HttpServerConfig();
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        conf.acceptors = new AcceptorConfig[]{ac};
        return conf;
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException e) {
            log.error("Can't send response", session, e);
        }
    }

    /**
     * Get, Delete or Put data by key.
     *
     * @param id      - key.
     * @param session - session.
     * @param replicas - number of nodes.
     * @param request - request.
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @Param("request") final Request request,
                       final HttpSession session) {
        final CompletableFuture<Response> response;
        if (id.isEmpty()) {
            basicFuctions.trySendResponse(session, CompletableFuture.supplyAsync(() ->
                    new Response(Response.BAD_REQUEST, Response.EMPTY)));
            return;
        }
        if (request.getHeader(PROXY_HEADER) == null) {
            response = sendToReplicas(id, replicas, request);
        } else {
            response = processRequest(request, id);
        }
        basicFuctions.trySendResponse(session, response);
    }

    private CompletableFuture<Response> sendToReplicas(final @Param(value = "id", required = true) String key,
                                                       final String replicas,
                                                       final @Param("request") Request request) {
        final Set<String> requestNodes;
        final List<CompletableFuture<Response>> answers = new ArrayList<>();
        final Replica replicasInfo = (replicas == null)
                ? defaultReplicas : new Replica(replicas);
        if (replicasInfo.getAckCount() <= 0
                || replicasInfo.getAckCount() > replicasInfo.getFromCount()) {
            return CompletableFuture.supplyAsync(() ->
                    new Response(Response.BAD_REQUEST, Response.EMPTY));
        } else {
            requestNodes = Util.getNodes(topology, key, replicasInfo.getFromCount());
            for (final String requestNode : requestNodes) {
                if (simpleTopology.isCurrentNode(requestNode)) {
                    answers.add(processRequest(request, key));
                } else {
                    answers.add(simpleTopology.forwardRequest(requestNode, request));
                }
            }
            return Futures.getAckResponses(replicasInfo.getAckCount(),answers)
                    .thenApplyAsync(resp ->
                                    replicasInfo.formFinalResponse(resp,request.getMethod()),
                            execPool)
                    .exceptionally(t -> {
                        log.error("Not Enough Replicas", t);
                        return new Response(
                                "504",
                                "Not Enough Replicas".getBytes(UTF_8));
                    });
        }
    }

    private CompletableFuture<Response> processRequest(final @Param("request") Request request,
                                                       final @Param(value = "id", required = true) String id) {
        switch (request.getMethod()) {
            case METHOD_GET:
                return basicFuctions.get(id);
            case METHOD_PUT:
                return basicFuctions.put(id, request.getBody());
            case METHOD_DELETE:
                return basicFuctions.delete(id);
            default:
                log.error("Unknown method");
                return CompletableFuture.supplyAsync(() -> new Response(
                        Response.METHOD_NOT_ALLOWED,
                        Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}