package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Charsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Simple Http Server Service implementation.
 *
 * @author Kate Moreva
 */

public class MySimpleHttpServer extends HttpServer implements Service {
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private static final String PROXY_HEADER = "X-Proxy-For:";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);
    private final ExecutorService executorService;
    private final Topology<String> topology;
    private final Map<String, HttpClient> nodeClients;
    private final MyRequestHelper requestHelper;
    private final Replicas quorum;

    /**
     * Http Server constructor.
     */
    public MySimpleHttpServer(final int port,
                              final DAO dao,
                              final int numberOfWorkers,
                              final int queueSize,
                              final Topology<String> topology) throws IOException {
        super(getConfig(port, numberOfWorkers));
        this.topology = topology;
        assert numberOfWorkers > 0;
        assert queueSize > 0;
        this.nodeClients = new HashMap<>();
        this.requestHelper = new MyRequestHelper(dao);
        for (final String node : topology.all()) {
            if (topology.isMe(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (nodeClients.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }
        this.executorService = new ThreadPoolExecutor(numberOfWorkers,
                queueSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in {} when processing request", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
        this.quorum = Replicas.quorum(nodeClients.size() + 1);
    }

    private static HttpServerConfig getConfig(final int port, final int numberOfWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numberOfWorkers;
        return config;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Method to check whether the server is reachable or not.
     * If the server is available @return {@link Response} {@code 200}.
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        requestHelper.sendLoggedResponse(session, new Response(Response.OK, Response.EMPTY));
    }

    /**
     * Method for working with value in the DAO by the key.
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 201} (new data created).
     * {@code 202} (data deleted).
     * {@code 405} (unexpected method).
     * {@code 500} (internal server error occurred).
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id, final Request request,
                       final HttpSession session, @Param("replicas") final String replicas) {
        try {
            executorService.execute(() -> {
                if (id.isBlank()) {
                    log.error("Request with empty id on /v0/entity");
                    requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final boolean isProxy = requestHelper.isProxied(request);
                final Replicas replicasFactor = isProxy || replicas == null ? this.quorum : Replicas.parser(replicas);
                if (replicasFactor.getAck() > replicasFactor.getFrom() || replicasFactor.getAck() <= 0) {
                    requestHelper.sendLoggedResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
                defineMethod(request, session, key, replicasFactor, isProxy);
            });
        } catch (RejectedExecutionException e) {
            requestHelper.sendLoggedResponse(session, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    private void defineMethod(final Request request, final HttpSession session, final ByteBuffer key,
                              final Replicas replicasFactor, final boolean isProxy) {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                getMethod(key, request, session, replicasFactor, isProxy);
                break;
            case Request.METHOD_PUT:
                putMethod(key, request, session, replicasFactor, isProxy);
                break;
            case Request.METHOD_DELETE:
                deleteMethod(key, request, session, replicasFactor, isProxy);
                break;
            default:
                log.error("Not allowed method on /v0/entity");
                requestHelper.sendLoggedResponse(session, new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                break;
        }
    }

    private void getMethod(final ByteBuffer key,
                           final Request request,
                           final HttpSession session,
                           final Replicas replicas,
                           final boolean isProxy) {
        if (isProxy) {
            requestHelper.sendLoggedResponse(session, requestHelper.getEntity(key));
            return;
        }
        final List<Response> result = replication(requestHelper.getEntity(key), request, key, replicas)
                .stream()
                .filter(resp -> requestHelper.getStatus(resp) == 200 || requestHelper.getStatus(resp) == 404)
                .collect(Collectors.toList());
        if (result.size() < replicas.getAck()) {
            requestHelper.sendLoggedResponse(session, new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY));
            return;
        }
        requestHelper.sendLoggedResponse(session, requestHelper.mergeResponses(result));
    }

    private void putMethod(final ByteBuffer key, final Request request, final HttpSession session,
                           final Replicas replicas, final boolean isProxy) {
        if (isProxy) {
            requestHelper.sendLoggedResponse(session, requestHelper.putEntity(key, request));
            return;
        }
        final List<Response> result = replication(requestHelper.putEntity(key, request), request, key, replicas)
                .stream()
                .filter(response -> requestHelper.getStatus(response) == 201)
                .collect(Collectors.toList());
        requestHelper.correctReplication(result.size(), replicas, session, Response.CREATED);
    }

    private void deleteMethod(final ByteBuffer key, final Request request, final HttpSession session,
                              final Replicas replicas, final boolean isProxy) {
        if (isProxy) {
            requestHelper.sendLoggedResponse(session, requestHelper.deleteEntity(key));
            return;
        }
        final List<Response> result = replication(requestHelper.deleteEntity(key), request, key, replicas)
                .stream()
                .filter(response -> requestHelper.getStatus(response) == 202)
                .collect(Collectors.toList());
        requestHelper.correctReplication(result.size(), replicas, session, Response.ACCEPTED);
    }

    private List<Response> replication(final Response response, final Request request, final ByteBuffer key,
                                       final Replicas replicas) {
        final Set<String> nodes = topology.primaryFor(key, replicas);
        final List<Response> result = new ArrayList<>(nodes.size());
        for (final String node : nodes) {
            if (topology.isMe(node)) {
                result.add(response);
            } else {
                result.add(proxy(node, request));
            }
        }
        return result;
    }

    private Response proxy(final String node, final Request request) {
        try {
            request.addHeader(PROXY_HEADER + node);
            return nodeClients.get(node).invoke(request);
        } catch (IOException | InterruptedException | PoolException | HttpException e) {
            log.error("Proxy request error", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error can't shutdown execution service");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : nodeClients.values()) {
            client.clear();
        }
    }
}
