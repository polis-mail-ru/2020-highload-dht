package ru.mail.polis.service.nik27090;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;
import static ru.mail.polis.service.nik27090.HttpHelper.sendResponse;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String REJECTED_EXECUTION_EXCEPTION = "Executor has been shut down or"
            + "executor uses finite bounds for both maximum threads and work queue capacity";

    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodeToClient;
    @NotNull
    private final DaoHelper daoHelper;
    private final String NOT_ENOUGH_REPLICAS = "Not enough replicas error with ack: {}, from: {}";

    /**
     * Service constructor.
     *
     * @param port          - port
     * @param dao           - key-value database
     * @param workers       - count of workers
     * @param queueCapacity - ArrayBlockingQueue capacity
     * @throws IOException - extend exception from HttpServer constructor
     */
    public ServiceImpl(
            final int port,
            final @NotNull DAO dao,
            final int workers,
            final int queueCapacity,
            final String timeout,
            @NotNull final Topology<String> topology) throws IOException {
        super(createConfig(port));
        this.topology = topology;
        this.nodeToClient = new HashMap<>();
        this.daoHelper = new DaoHelper(dao);

        for (final String node : topology.all()) {
            if (topology.isCurrentNode(node)) {
                continue;
            }

            final HttpClient client = new HttpClient(new ConnectionString(node + timeout));
            if (nodeToClient.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }
        executorService = new ThreadPoolExecutor(
                workers, queueCapacity,
                60_000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Exception {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        log.debug("Request status.");

        sendResponse(session, Response.ok("OK"));
    }

    /**
     * Get/Delete/Put data by key.
     *
     * @param id      - key for storage
     * @param session - session
     * @param request - request
     */
    @Path("/v0/entity")
    @RequestMethod({METHOD_GET, METHOD_DELETE, METHOD_PUT})
    public void requestHandler(
            @NotNull final @Param(value = "id", required = true) String id,
            final @Param(value = "replicas") String af,
            final HttpSession session,
            final Request request) {
        try {
            executorService.execute(() -> {
                AckFrom ackFrom = topology.parseAckFrom(af);
                if (ackFrom.getAck() > ackFrom.getFrom() || ackFrom.getAck() <= 0) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }

                if (id.isEmpty()) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }

                switch (request.getMethod()) {
                    case METHOD_GET:
                        log.debug("GET request: id = {}", id);
                        getEntityExecutor(id, session, request, ackFrom);
                        break;
                    case METHOD_PUT:
                        log.debug("PUT request: id = {}, value length = {}", id, request.getBody().length);
                        putEntityExecutor(id, session, request, ackFrom);
                        break;
                    case METHOD_DELETE:
                        log.debug("DELETE request: id = {}", id);
                        deleteEntityExecutor(id, session, request, ackFrom);
                        break;
                    default:
                        break;
                }
            });
        } catch (RejectedExecutionException e) {
            log.error(REJECTED_EXECUTION_EXCEPTION, e);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE));
        }
    }

    private void getEntityExecutor(String id, HttpSession session, Request request, AckFrom ackFrom) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            sendResponse(session, daoHelper.getEntity(key));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.getEntity(key), nodeToClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.OK
                        || response.getStatus() == ResponseCode.NOT_FOUND)
                .collect(Collectors.toList());
        if (notFailedResponses.size() < ackFrom.getAck()) {
            log.error(NOT_ENOUGH_REPLICAS, ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } else {
            sendResponse(session, daoHelper.resolveGet(notFailedResponses));
        }
    }

    private void deleteEntityExecutor(String id, HttpSession session, Request request, AckFrom ackFrom) {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            sendResponse(session, daoHelper.delEntity(key));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.delEntity(key), nodeToClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.ACCEPTED)
                .collect(Collectors.toList());

        if (notFailedResponses.size() < ackFrom.getAck()) {
            log.error(NOT_ENOUGH_REPLICAS, ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
        }
    }

    private void putEntityExecutor(final String id,
                                   final HttpSession session,
                                   final Request request,
                                   @NotNull AckFrom ackFrom) {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        if (topology.isProxyReq(request)) {
            sendResponse(session, daoHelper.putEntity(key, value));
            return;
        }

        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom()))
                .collect(Collectors.toList());

        final List<Response> notFailedResponses = topology
                .getResponseFromNodes(nodes, request, daoHelper.putEntity(key, value), nodeToClient)
                .stream()
                .filter(response -> response.getStatus() == ResponseCode.CREATED)
                .collect(Collectors.toList());
        if (notFailedResponses.size() < ackFrom.getAck()) {
            log.error(NOT_ENOUGH_REPLICAS, ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } else {
            sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        log.debug("Can't understand request: {}", request);

        sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("ERROR. Cant shutdown executor.", e);
            Thread.currentThread().interrupt();
        }

        for (final HttpClient client : nodeToClient.values()) {
            client.clear();
        }
    }
}
