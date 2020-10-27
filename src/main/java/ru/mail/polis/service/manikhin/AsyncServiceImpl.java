package ru.mail.polis.service.manikhin;

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
import ru.mail.polis.dao.manikhin.ByteConvertor;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.TimestampRecord;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final String PROXY_HEADER = "X-OK-Proxy: True";
    private static final String ENTITY_HEADER = "/v0/entity?id=";

    private final DAO dao;
    private final ExecutorService executor;
    private final Topology nodes;
    private final Map<String, HttpClient> clusterClients = new HashMap<>();
    private final Replicas defaultReplica;
    private final int clusterSize;
    private final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);

    /**
     * AsyncService initialization.
     *
     * @param port - to accept HTTP server
     * @param dao - storage interface
     */
    public AsyncServiceImpl(final int port, @NotNull final DAO dao,
                            @NotNull final Topology nodes, final int countOfWorkers,
                            final int queueSize, final int timeout) throws IOException {

        super(getConfig(port));
        this.dao = dao;
        this.executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").setUncaughtExceptionHandler((t, e) ->
                        log.error("Error in {} when processing request", t, e)
                ).build(),
                new ThreadPoolExecutor.AbortPolicy());
        this.nodes = nodes;
        this.clusterSize = nodes.getNodes().size();
        this.defaultReplica = Replicas.quorum(clusterSize);

        for (final String node : nodes.getNodes()) {
            if (!nodes.getId().equals(node) && !clusterClients.containsKey(node)) {
                this.clusterClients.put(node, new HttpClient(new ConnectionString(node + "?timeout=" + timeout)));
            }
        }
    }

    private Response getTimestamp(@NotNull final ByteBuffer key) {
        try {
            final byte[] res = timestampFromByteBuffer(key);
            return new Response(Response.OK, res);
        } catch (NoSuchElementException | IOException exp) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private void putTimestamp(@NotNull final ByteBuffer key,
                              @NotNull final Request request) throws IOException {
        dao.upsertTimestampRecord(key, ByteBuffer.wrap(request.getBody()));
    }

    private void deleteTimestamp(@NotNull final ByteBuffer key) throws IOException {
        dao.removeTimestampRecord(key);
    }

    private byte[] timestampFromByteBuffer(@NotNull final ByteBuffer key)
            throws IOException {
        final TimestampRecord res = dao.getTimestampRecord(key);
        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }
        return res.toBytes();
    }

    /**
     * Http status getter path.
     *
     * @param session - HttpSession
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        try {
            session.sendResponse(Response.ok("OK"));
        } catch (IOException error) {
            log.error("Can't send response. Error: ", error);
        }
    }

    /**
     * Provide access to entities.
     *
     * @param id      key of entity
     * @param request HTTP request
     * @param session - HttpSession
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @NotNull final Request request,
                       @NotNull final HttpSession session,
                       @Param(value= "replicas") final String replicas) {

        try {
            if (id.isEmpty()) {
                sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            }
            final boolean isForwardedRequest = request.getHeader(PROXY_HEADER) != null;
            final Replicas replicaFactor = Replicas.ReplicaFactor(replicas, session, defaultReplica, clusterSize);
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

            if (isForwardedRequest || clusterSize > 1) {
                final Set<String> replicaClusters = isForwardedRequest ? Collections.singleton(nodes.getId())
                        : nodes.getReplicas(key, replicaFactor);

                handleMultiRequest(replicaClusters, request, replicaFactor.getAck(), session);
            } else {
                final Future<?> future = executor.submit(() -> handleSingleRequest(request, session, key));

                if (future.isCancelled()) {
                    log.error("Executor error!");
                }
            }

        } catch (RejectedExecutionException | IOException error) {
            log.error("RejectedExecution error: ", error);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    public void handleMultiRequest(@NotNull final Set<String> replicaClusters,
                                   @NotNull final Request request,
                                   final int replicateAcks,
                                   @NotNull final HttpSession session) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    multiGet(session, replicaClusters, request, replicateAcks);
                    break;
                case Request.METHOD_PUT:
                    multiPut(session, replicaClusters, request, replicateAcks);
                    break;
                case Request.METHOD_DELETE:
                    multiDelete(session, replicaClusters, request, replicateAcks);
                    break;
                default:
                    sendResponse(session, new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    break;
            }
        } catch (IOException error) {
            log.error("handleMultiRequest error: ", error);
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    /**
     * Request handler for input requests.
     *
     * @param request - input http-request
     * @param session - http-session
     * @param key - input key
     */
    private void handleSingleRequest(@NotNull final Request request,
                                     @NotNull final HttpSession session,
                                     @NotNull final ByteBuffer key) {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                get(key, session);
                break;
            case Request.METHOD_PUT:
                put(key, request, session);
                break;
            case Request.METHOD_DELETE:
                delete(key, session);
                break;
            default:
                sendResponse(session, new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                break;
        }
    }

    /**
     * Response sender for input requests.
     *
     * @param session - http-session
     * @param response - response object on input request
     */
    private void sendResponse(@NotNull final HttpSession session,
                              @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException error) {
            log.error("Sending response error: ", error);
        }
    }

    private void get(@NotNull final ByteBuffer key, @NotNull final HttpSession session) {
        try {
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] valueArray = ByteConvertor.toArray(value);

            sendResponse(session, Response.ok(valueArray));

        } catch (final IOException error) {
            log.error("IO get error: ", error);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        } catch (NoSuchElementException error) {
            log.error("NoSuchElement get error: ", error);
            sendResponse(session, new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    public void multiGet(@NotNull final HttpSession session,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final Request request,
                         final int replicateAcks) throws IOException {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        final List<TimestampRecord> responses = new ArrayList<>();

        for (final String node : replicaNodes) {
            try {
                Response respGet;

                if (node.equals(nodes.getId())) {
                    respGet = getTimestamp(key);
                } else {
                    respGet = clusterClients.get(node).get(ENTITY_HEADER + id, PROXY_HEADER);
                }

                if (respGet.getStatus() == 404 && respGet.getBody().length == 0) {
                    responses.add(TimestampRecord.getEmpty());
                } else if (respGet.getStatus() == 500) {
                    continue;
                } else {
                    responses.add(TimestampRecord.fromBytes(respGet.getBody()));
                }

                asks++;
            } catch (HttpException | PoolException | InterruptedException error) {
                log.error("multiGet error", error);
            }
        }

        final boolean isForwardedRequest = request.getHeader(PROXY_HEADER) != null;

        if (asks >= replicateAcks || isForwardedRequest) {
            processResponses(session, replicaNodes, responses, isForwardedRequest);
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    private void processResponses(@NotNull final HttpSession session,
                                  @NotNull final Set<String> replicaNodes,
                                  @NotNull final List<TimestampRecord> responses,
                                  final boolean isForwardedRequest) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);

        if (mergedResp.isValue()) {
            if (!isForwardedRequest && replicaNodes.size() == 1) {
                sendResponse(session, new Response(Response.OK, mergedResp.getValueAsBytes()));
            } else if (isForwardedRequest && replicaNodes.size() == 1) {
                sendResponse(session, new Response(Response.OK, mergedResp.toBytes()));
            } else {
                sendResponse(session, new Response(Response.OK, mergedResp.getValueAsBytes()));
            }
        } else if (mergedResp.isDeleted()) {
            sendResponse(session, new Response(Response.NOT_FOUND, mergedResp.toBytes()));
        } else {
            sendResponse(session, new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    private void put(final ByteBuffer key, final Request request,
                     final HttpSession session) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException error) {
            log.error("IO put error: ", error);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    public void multiPut(@NotNull final HttpSession session,
                         @NotNull final Set<String> replicaNodes,
                         @NotNull final Request request,
                         final int replicateAcks) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    putTimestamp(key, request);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node)
                            .put(ENTITY_HEADER + id, request.getBody(), PROXY_HEADER);
                    if (resp.getStatus() == 201) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | PoolException | InterruptedException error) {
                log.error("multiPut error", error);
            }
        }
        if (asks >= replicateAcks || request.getHeader(PROXY_HEADER) != null) {
            sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    private void delete(@NotNull final ByteBuffer key,
                        @NotNull final HttpSession session) {
        try {
            dao.remove(key);
            sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException error) {
            log.error("IO delete error: ", error);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    public void multiDelete(@NotNull final HttpSession session,
                            @NotNull Set<String> replicaNodes,
                            @NotNull final Request request,
                            final int replicateAcks) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        int asks = 0;

        for (final String node : replicaNodes) {
            try {
                if (node.equals(nodes.getId())) {
                    deleteTimestamp(key);
                    asks++;
                } else {
                    final Response resp = clusterClients.get(node)
                            .delete(ENTITY_HEADER + id, PROXY_HEADER);
                    if (resp.getStatus() == 202) {
                        asks++;
                    }
                }
            } catch (IOException | HttpException | InterruptedException | PoolException error) {
                log.warn("multiDelete error: ", error);
            }
        }
        if (asks >= replicateAcks || request.getHeader(PROXY_HEADER) != null) {
            sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException error) {
            log.error("Handle error: ", error);
        }
    }

    private static HttpServerConfig getConfig(final int port) {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            log.error("Can't stop server! Error: ", error);
            Thread.currentThread().interrupt();
        }

        for (final HttpClient client : clusterClients.values()) {
            client.clear();
        }
    }
}
