package ru.mail.polis.service.dariagap;

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
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Replicas;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ClusterServiceImpl extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    private final ExecutorService exec;
    private final Set<String> topology;
    private final Map<String,HttpClient> clients;
    private final Logger log = LoggerFactory.getLogger(ClusterServiceImpl.class);
    private final String thisNode;

    private static final String PROXY_HEADER = "X-OK-Proxy: True";

    private static final String EXECUTOR_ERROR = "Request rejected";
    private static final String RESPONSE_ERROR = "Can not send response.";
    private static final String PROXY_ERROR = "Can not proxy request";
    private static final String INTERNAL_ERROR = "Internal Server Error";
    private static final String NOT_FOUND_ERROR = "Data not found";

    /**
     * Config HttpServer, DAO and ExecutorService.
     *
     * @param port - to accept HTTP server
     * @param dao - storage interface
     * @param executors - number of executors
     * @param queueSize - size of queue in ThreadPoolExecutor
     * @param topology - set of cluster nodes
     * @param timeout - timeout of connection
     */
    public ClusterServiceImpl(
            final int port,
            @NotNull final DAO dao,
            @NotNull final int executors,
            @NotNull final int queueSize,
            @NotNull final Set<String> topology,
            @NotNull final int timeout) throws IOException {
        super(formConfig(port));
        HttpClient client;
        this.dao = dao;
        this.exec = new ThreadPoolExecutor(
                executors,
                executors,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Executor-%d")
                        .build()
                );
        this.clients = new HashMap<>();
        this.topology = topology;
        this.thisNode = "http://localhost:" + this.port;
        for (final String s: topology) {
            if (!isCurrentNode(s)) {
                client = new HttpClient(new ConnectionString(s));
                client.setTimeout(timeout);
                this.clients.put(s,client);
            }
        }
    }

    private Boolean isCurrentNode(final String node) {
        return node.equals(this.thisNode);
    }

    private static HttpServerConfig formConfig(final int port) {
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
        sendResponse(session, Response.ok("OK"));
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
        try {
            exec.execute(() -> {
                final Response response;
                if (request.getHeader(PROXY_HEADER) == null) {
                    response = getResponseFromGateNode(id, replicas, request);
                } else {
                    response = getResponseFromCurrent(request, id);
                }
                sendResponse(session, response);
            });
        } catch (RejectedExecutionException ex) {
            sendResponse(session,new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            log.error(EXECUTOR_ERROR, ex);
        }
    }

    private Response getResponseFromGateNode(final String id,
                                              final String replicas,
                                              final Request request) {
        final Set<String> requestNodes;
        final Response response;
        final Replicas replicasInfo = (replicas == null) ?
                new Replicas(topology.size()) : new Replicas(replicas);
        if (id.isEmpty() || (replicasInfo.getAsk() <= 0) ||
                (replicasInfo.getAsk() > replicasInfo.getFrom())) {
            response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        } else {
            requestNodes = Util.getNodes(topology, id, replicasInfo.getFrom());
            for (String requestNode : requestNodes) {
                if (isCurrentNode(requestNode)) {
                    replicasInfo.analyseResponse(
                            getResponseFromCurrent(request, id),
                            request.getMethod());
                } else {
                    replicasInfo.analyseResponse(
                            proxy(clients.get(requestNode), request),
                            request.getMethod());
                }
            }
            response = replicasInfo.formFinalResponse(request.getMethod());
        }
        return response;
    }

    private Response getResponseFromCurrent (final Request request, final String id) {
        final Response response;
        switch (request.getMethod()) {
            case METHOD_GET:
                response = getSync(id);
                break;
            case METHOD_PUT:
                response = putSync(id, request.getBody());
                break;
            case METHOD_DELETE:
                response = deleteSync(id);
                break;
            default:
                log.error("Unknown method");
                response = new Response(
                        Response.METHOD_NOT_ALLOWED,
                        Response.EMPTY);
                break;
        }
        return response;
    }

    private void sendResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException ex) {
            log.error(RESPONSE_ERROR, ex);
        }
    }

    private Response proxy(final HttpClient client, final Request request) {
        try {
            request.addHeader(PROXY_HEADER);
            return client.invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException e) {
            log.error(PROXY_ERROR, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response getSync(final String id) {
        try {
            final ByteBuffer value = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.OK, Util.byteBufferToBytes(value));
        } catch (IOException ex) {
            log.error(INTERNAL_ERROR, ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException ex) {
            log.error(NOT_FOUND_ERROR, ex);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private Response putSync(final String id,
                         final byte[] body) {
        try {
            dao.upsertWithTimestamp(
                    ByteBuffer.wrap(id.getBytes(UTF_8)),
                    ByteBuffer.wrap(body));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ex) {
            log.error(INTERNAL_ERROR, ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response deleteSync(final String id) {
        try {
            dao.removeWithTimestamp(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException ex) {
            log.error(INTERNAL_ERROR, ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        sendResponse(session,new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Can not stop server.", ex);
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clients.values()) {
            client.clear();
        }
    }
}
