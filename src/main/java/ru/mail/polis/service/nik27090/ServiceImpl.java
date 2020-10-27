package ru.mail.polis.service.nik27090;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.nik27090.Cell;
import ru.mail.polis.dao.nik27090.Value;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String REJECTED_EXECUTION_EXCEPTION = "Executor has been shut down or"
            + "executor uses finite bounds for both maximum threads and work queue capacity";
    private static final String PROXY_HEADER = "Timestamp:";


    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodeToClient;

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
        this.dao = dao;
        this.topology = topology;
        this.nodeToClient = new HashMap<>();

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
    public void getEntity(
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
        final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom())).collect(Collectors.toList());

        if (topology.isProxyReq(request)) {
            sendResponse(session, getEntity(key));
            return;
        }

        List<Response> notFailedResponses = getResponseFromNodes(nodes, request, getEntity(key))
                .stream()
                .filter(response -> response.getStatus() == 200
                        || response.getStatus() == 404)
                .collect(Collectors.toList());
        if (notFailedResponses.size() < ackFrom.getAck()) {
            log.error("Not enough replicas error with ack: {}, from: {}", ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } else {
            sendResponse(session, resolveGet(notFailedResponses));
        }
    }

    private void deleteEntityExecutor(String id, HttpSession session, Request request, AckFrom ackFrom) {
        try {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom())).collect(Collectors.toList());

            if (topology.isProxyReq(request)) {
                dao.remove(key);
                sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
                return;
            }

            List<Response> notFailedResponses = getResponseFromNodes(nodes, request, delEntity(key))
                    .stream()
                    .filter(response -> response.getStatus() == 202)
                    .collect(Collectors.toList());

            if (notFailedResponses.size() < ackFrom.getAck()) {
                log.error("Not enough replicas error with ack: {}, from: {}", ackFrom.getAck(), ackFrom.getFrom());
                sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
            } else {
                sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
            }
        } catch (IOException e) {
            log.error("Internal error with id = {}", id, e);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void putEntityExecutor(final String id, final HttpSession session, final Request request, @NotNull AckFrom ackFrom) {
        try {
            final ByteBuffer value = ByteBuffer.wrap(request.getBody());
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final List<String> nodes = Arrays.stream(topology.getReplicas(key, ackFrom.getFrom())).collect(Collectors.toList());

            if (topology.isProxyReq(request)) {
                dao.upsert(key, value);
                sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
                return;
            }

            List<Response> notFailedResponses = getResponseFromNodes(nodes, request, putEntity(key, value))
                    .stream()
                    .filter(response -> response.getStatus() == 201)
                    .collect(Collectors.toList());
            if (notFailedResponses.size() < ackFrom.getAck()) {
                log.error("Not enough replicas error with ack: {}, from: {}", ackFrom.getAck(), ackFrom.getFrom());
                sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
            } else {
                sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
            }
        } catch (IOException e) {
            log.error("Internal error with id = {}, value length = {}", id, request.getBody().length, e);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private Response resolveGet(List<Response> result) {
        final Map<Response, Integer> responses = new HashMap<>();
        result.forEach(resp -> {
            final Integer val = responses.get(resp);
            responses.put(resp, val == null ? 0 : val + 1);
        });
        Response finalResult = null;
        int maxCount = -1;
        long time = Long.MIN_VALUE;
        for (final Map.Entry<Response, Integer> entry : responses.entrySet()) {
            if (entry.getValue() >= maxCount && getTimestamp(entry.getKey()) > time) {
                time = getTimestamp(entry.getKey());
                maxCount = entry.getValue();
                finalResult = entry.getKey();
            }
        }
        return finalResult;
    }

    public static long getTimestamp(final Response response) {
        final String timestamp = response.getHeader(PROXY_HEADER);
        return timestamp == null ? -1L : Long.parseLong(timestamp);
    }

    public Response getEntity(final ByteBuffer key) {
        final Cell cell;
        try {
            cell = dao.getCell(key);
            Value cellValue = cell.getValue();
            if (cellValue.isTombstone()) {
                final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                response.addHeader(PROXY_HEADER + cellValue.getTimestamp());
                return response;
            }
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] body = new byte[value.remaining()];
            value.get(body);
            final Response response = new Response(Response.OK, body);
            response.addHeader(PROXY_HEADER + cell.getValue().getTimestamp());
            return response;
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET method failed on /v0/entity for id {}", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response delEntity(ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            log.error("DELETE Internal error with key = {}", key, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response putEntity(ByteBuffer key, ByteBuffer value) {
        try {
            dao.upsert(key, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            log.error("PUT Internal error.", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private List<Response> getResponseFromNodes(List<String> nodes, Request request, Response localResponse) {
        final List<Response> responses = new ArrayList<>(nodes.size());
        for (String node : nodes) {
            if (topology.isCurrentNode(node)) {
                responses.add(localResponse);
            } else {
                responses.add(proxy(node, request));
            }
        }
        return responses;
    }

    @NotNull
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) {
        request.addHeader("X-Proxy-For: " + node);
        try {
            return nodeToClient.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException | IOException e) {
            log.error("Can't proxy request", e);
            return new Response(Response.INTERNAL_ERROR);
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

    private void sendResponse(@NotNull final HttpSession session, @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error("Can't send response", e);
        }
    }
}
