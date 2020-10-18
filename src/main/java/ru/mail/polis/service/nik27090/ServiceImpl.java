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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String REJECTED_EXECUTION_EXCEPTION = "Executor has been shut down or"
            + "executor uses finite bounds for both maximum threads and work queue capacity";

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
            @NotNull final Topology<String> topology) throws IOException {
        super(createConfig(port));
        this.dao = dao;
        this.topology = topology;
        this.nodeToClient = new HashMap<>();
        for (final String node : topology.all()) {
            if (topology.isCurrentNode(node)) {
                continue;
            }

            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (nodeToClient.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }
        executorService = new ThreadPoolExecutor(
                workers, queueCapacity,
                0L, TimeUnit.MILLISECONDS,
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
            final HttpSession session,
            final Request request) {
        try {
            executorService.execute(() -> {

                if (id.isEmpty()) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                switch (request.getMethod()) {
                    case METHOD_GET:
                        log.debug("GET request: id = {}", id);
                        getEntityExecutor(id, session, request);
                        break;
                    case METHOD_PUT:
                        log.debug("PUT request: id = {}, value length = {}", id, request.getBody().length);
                        putEntityExecutor(id, session, request);
                        break;
                    case METHOD_DELETE:
                        log.debug("DELETE request: id = {}", id);
                        deleteEntityExecutor(id, session, request);
                        break;
                }
            });
        } catch (RejectedExecutionException e) {
            log.error(REJECTED_EXECUTION_EXCEPTION, e);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE));
        }
    }

    private void getEntityExecutor(final String id, final HttpSession session, final Request request) {
        try {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final String node = topology.getRightNodeForKey(key);
            if (topology.isCurrentNode(node)) {
                final ByteBuffer value = dao.get(key).duplicate();
                final byte[] response = new byte[value.remaining()];
                value.get(response);
                sendResponse(session, Response.ok(response));
            } else {
                sendResponse(session, proxy(node, request));
            }
        } catch (NoSuchElementException e) {
            sendResponse(session, new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException e) {
            log.error("Internal error with id = {}", id, e);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void deleteEntityExecutor(final String id, final HttpSession session, final Request request) {
        try {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final String node = topology.getRightNodeForKey(key);
            if (topology.isCurrentNode(node)) {
                dao.remove(key);
                sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
            } else {
                sendResponse(session, proxy(node, request));
            }
        } catch (IOException e) {
            log.error("Internal error with id = {}", id, e);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void putEntityExecutor(final String id, final HttpSession session, final Request request) {
        try {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final String node = topology.getRightNodeForKey(key);
            if (topology.isCurrentNode(node)) {
                final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                dao.upsert(key, value);
                sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
            } else {
                sendResponse(session, proxy(node, request));
            }
        } catch (IOException e) {
            log.error("Internal error with id = {}, value length = {}", id, request.getBody().length, e);
            sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    @NotNull
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) throws IOException {
        request.addHeader("X-Proxy-For: " + node);
        try {
            return nodeToClient.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            throw new IOException("Can't proxy request", e);
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
