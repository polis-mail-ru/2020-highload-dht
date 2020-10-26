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
import ru.mail.polis.dao.kate.moreva.Cell;
import ru.mail.polis.dao.kate.moreva.Value;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private static final String SERVER_ERROR = "Server error can't send response";
    private static final String TIMESTAMP = "Timestamp: ";
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private static final String PROXY_HEADER = "X-Proxy-For:";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);
    private final DAO dao;
    private final ExecutorService executorService;
    private final Topology<String> topology;
    private final Map<String, HttpClient> nodeClients;
    private final MyRequestHelper requestHelper;

    /**
     * Http Server constructor.
     */
    public MySimpleHttpServer(final int port,
                              final DAO dao,
                              final int numberOfWorkers,
                              final int queueSize,
                              final Topology<String> topology) throws IOException {
        super(getConfig(port, numberOfWorkers));
        this.dao = dao;
        this.topology = topology;
        assert numberOfWorkers > 0;
        assert queueSize > 0;
        this.nodeClients = new HashMap<>();
        this.requestHelper = new MyRequestHelper();
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
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }

    /**
     * Method to check whether the server is reachable or not.
     * If the server is available @return {@link Response} {@code 200}.
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.OK, Response.EMPTY));
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
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
    public void entity(@Param(value = "id", required = true) final String id,
                       final Request request,
                       final HttpSession session, @Param("replicas") final String replicas) {
        try {
            executorService.execute(() -> {
                if (id.isBlank()) {
                    log.error("Request with empty id on /v0/entity");
                    handleError(session, Response.BAD_REQUEST);
                    return;
                }
                final boolean isProxy = requestHelper.isProxied(request);
                final Replicas replicasFactor = isProxy
                        || replicas == null ? Replicas.quorum(nodeClients.size() + 1) : Replicas.parser(replicas);

                if (replicasFactor.getAck() > replicasFactor.getFrom() || replicasFactor.getAck() <= 0) {
                    handleError(session, Response.BAD_REQUEST);
                    return;
                }
                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));

                defineMethod(request, session, key, replicasFactor, isProxy);
            });
        } catch (RejectedExecutionException e) {
            handleError(session, Response.SERVICE_UNAVAILABLE);
        }
    }

    private void defineMethod(final Request request,
                              final HttpSession session,
                              final ByteBuffer key,
                              final Replicas replicasFactor,
                              final boolean isProxy) {
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
                handleError(session, Response.METHOD_NOT_ALLOWED);
                break;
        }

    }

    private void getMethod(final ByteBuffer key,
                           final Request request,
                           final HttpSession session,
                           final Replicas replicas,
                           final boolean isProxy) {
        if (isProxy) {
            sendLoggedResponse(session, getEntity(key));
            return;
        }
        final List<Response> result = replication(getEntity(key), request, key, replicas)
                .stream()
                .filter(resp -> requestHelper.getStatus(resp).equals(Response.OK)
                        || requestHelper.getStatus(resp).equals(Response.NOT_FOUND))
                .collect(Collectors.toList());
        if (result.size() < replicas.getAck()) {
            handleError(session, NOT_ENOUGH_REPLICAS);
            return;
        }
        sendLoggedResponse(session, requestHelper.mergeResponses(result));
    }

    /**
     * Subsidiary method to get value.
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    private Response getEntity(final ByteBuffer key) {
        final Cell cell;
        try {
            cell = dao.getCell(key);
            final Value cellValue = cell.getValue();
            if (cellValue.isTombstone()) {
                final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                response.addHeader(TIMESTAMP + cellValue.getTimestamp());
                return response;
            }
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] body = new byte[value.remaining()];
            value.get(body);
            final Response response = new Response(Response.OK, body);
            response.addHeader(TIMESTAMP + cell.getValue().getTimestamp());
            return response;
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET method failed on /v0/entity for id {}", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

    }

    private void putMethod(final ByteBuffer key,
                           final Request request,
                           final HttpSession session,
                           final Replicas replicas,
                           final boolean isProxy) {
        if (isProxy) {
            sendLoggedResponse(session, putEntity(key, request));
            return;
        }
        final List<Response> result = replication(putEntity(key, request), request, key, replicas)
                .stream()
                .filter(response -> requestHelper.getStatus(response).equals(Response.CREATED))
                .collect(Collectors.toList());
        correctReplication(result.size(), replicas, session, Response.CREATED);
    }

    /**
     * Subsidiary method to put new value.
     * {@code 201} (new data created).
     * {@code 500} (internal server error occurred).
     */
    private Response putEntity(final ByteBuffer key, final Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            log.error("PUT method failed on /v0/entity for id {}, request body {}.", key.get(), request.getBody(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void deleteMethod(final ByteBuffer key,
                              final Request request,
                              final HttpSession session,
                              final Replicas replicas, final boolean isProxy) {
        if (isProxy) {
            sendLoggedResponse(session, deleteEntity(key));
            return;
        }
        final List<Response> result = replication(deleteEntity(key), request, key, replicas)
                .stream()
                .filter(response -> requestHelper.getStatus(response).equals(Response.ACCEPTED))
                .collect(Collectors.toList());
        correctReplication(result.size(), replicas, session, Response.ACCEPTED);
    }

    /**
     * Subsidiary method to delete value by the key.
     * {@code 202} (data deleted).
     * {@code 500} (internal server error occurred).
     */
    private Response deleteEntity(final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            log.error("DELETE method failed on /v0/entity for id {}.", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void correctReplication(final int ack,
                                    final Replicas replicas,
                                    final HttpSession session,
                                    final String response) {
        try {
            if (ack < replicas.getAck()) {
                handleError(session, NOT_ENOUGH_REPLICAS);
            } else {
                session.sendResponse(new Response(response, Response.EMPTY));
            }
        } catch (IOException e) {
            handleError(session, Response.INTERNAL_ERROR);
        }
    }

    private List<Response> replication(final Response response,
                                       final Request request,
                                       final ByteBuffer key,
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

    private void sendLoggedResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }

    private void handleError(final HttpSession session, final String response) {
        try {
            session.sendResponse(new Response(response, Response.EMPTY));
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
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
