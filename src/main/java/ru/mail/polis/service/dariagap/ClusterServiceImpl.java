package ru.mail.polis.service.dariagap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import one.nio.server.RejectedSessionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Replicas;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
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

public class ClusterServiceImpl extends HttpServer implements Service {
    private final BasicService basicService;
    private final ExecutorService execPool;
    private final Set<String> topology;
    private static final Logger log = LoggerFactory.getLogger(ClusterServiceImpl.class);
    private final String thisNode;
    private final Replicas defaultReplicas;
    private final DAO dao;

    private static final String PROXY_HEADER = "X-OK-Proxy";

    /**
     * Config BasicServer, DAO and ExecutorService.
     *
     * @param port - to accept HTTP server
     * @param dao - storage interface
     * @param executors - number of executors
     * @param queueSize - size of queue in ThreadPoolExecutor
     * @param topology - set of cluster nodes
     */
    public ClusterServiceImpl(
            final int port,
            @NotNull final DAO dao,
            @NotNull final int executors,
            @NotNull final int queueSize,
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
        this.basicService = new BasicService(dao,executors,execPool);
        this.topology = topology;
        this.thisNode = "http://localhost:" + this.port;
        this.defaultReplicas = new Replicas(topology.size());
        this.dao = dao;
    }

    private Boolean isCurrentNode(final String node) {
        return node.equals(this.thisNode);
    }

    private static HttpServerConfig newConfig(final int port) {
        final HttpServerConfig conf = new HttpServerConfig();
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        conf.acceptors = new AcceptorConfig[]{ac};
        return conf;
    }

    /**
     * Status OK answer.
     *
     * @param session - HttpSession
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        Util.sendResponse(session, Response.ok("OK"));
    }

    /**
     * Get range of data between {@code start} (inclusive) and optional {@code end}.
     */
    @Path("/v0/entities")
    public void entities(@Param(value = "start", required = true) final String idStart,
                       @Param(value = "end") final String idEnd,
                       final HttpSession session) {
        try {
            if (idStart.isEmpty() || ((idEnd != null) && idEnd.isEmpty())) {
                throw new IllegalArgumentException();
            }

            final ByteBuffer start = ByteBuffer.wrap(idStart.getBytes(UTF_8));
            final ByteBuffer end = (idEnd == null) ? null
                    : ByteBuffer.wrap(idEnd.getBytes(UTF_8));
            final Iterator<Record> iterator = dao.range(start,end);
            ((StreamSession) session).setIterator(iterator);
        } catch (IOException ex) {
            log.error("Can not send entities", ex);
        } catch (IllegalArgumentException ex) {
            Util.sendResponse(session,
                    new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
    }

    /**
     * Get, set or delete data by id.
     *
     * @param id key of entity
     * @param replicas number of nodes that must confirm the operation
     * @param request request with the entity value in body (for METHOD_PUT)
     * @param session - HttpSession
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @Param("request") final Request request,
                       final HttpSession session) {
        final CompletableFuture<Response> response;
        if (request.getHeader(PROXY_HEADER) == null) {
            response = getResponseFromGateNode(id, replicas, request);
        } else {
            response = getResponseFromCurrent(request, id);
        }
        Util.sendResponseFromFuture(session, response);
    }

    private CompletableFuture<Response> getResponseFromGateNode(final String id,
                                              final String replicas,
                                              final Request request) {
        final Set<String> requestNodes;
        final List<CompletableFuture<Response>> responses = new ArrayList<>();
        final Replicas replicasInfo = (replicas == null)
                ? defaultReplicas : new Replicas(replicas);
        if (id.isEmpty() || replicasInfo.getAck() <= 0
                || replicasInfo.getAck() > replicasInfo.getFrom()) {
            return CompletableFuture.supplyAsync(() ->
                    new Response(Response.BAD_REQUEST, Response.EMPTY));
        } else {
            requestNodes = Util.getNodes(topology, id, replicasInfo.getFrom());
            for (final String requestNode : requestNodes) {
                if (isCurrentNode(requestNode)) {
                    responses.add(getResponseFromCurrent(request, id));
                } else {
                    responses.add(basicService.proxy(requestNode, request));
                }
            }
            return Futures.getAckResponses(replicasInfo.getAck(),responses)
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

    private CompletableFuture<Response> getResponseFromCurrent(final Request request,
                                                               final String id) {
        switch (request.getMethod()) {
            case METHOD_GET:
                return basicService.get(id);
            case METHOD_PUT:
                return basicService.put(id, request.getBody());
            case METHOD_DELETE:
                return basicService.delete(id);
            default:
                log.error("Unknown method");
                return CompletableFuture.supplyAsync(() -> new Response(
                        Response.METHOD_NOT_ALLOWED,
                        Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        Util.sendResponse(session,new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public HttpSession createSession(Socket socket) throws RejectedSessionException {
        return new StreamSession(socket, this);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        execPool.shutdown();
        try {
            execPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Can not stop server.", ex);
            Thread.currentThread().interrupt();
        }
        basicService.stop();
    }
}
