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
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.manikhin.ByteConvertor;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncServiceImpl extends HttpServer implements Service {

    private final DAO dao;
    private final ExecutorService executor;
    private final Topology nodes;
    private final Map<String, HttpClient> clusterClients = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);

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

        for (final String node : nodes.getNodes()) {
            if (!nodes.getId().equals(node) && !clusterClients.containsKey(node)) {
                this.clusterClients.put(node, new HttpClient(new ConnectionString(node + "?timeout=" + timeout)));
            }
        }
    }

    /**
     * Request forwarder on specified node.
     *
     * @param session - http-session
     * @param nodeId - input node identifier
     * @param request - input http-request
     */
    private void forwardRequest(@NotNull final HttpSession session,
                                @NotNull final String nodeId,
                                @NotNull final Request request) {

        request.addHeader("X-Proxy-For: " + nodeId);

        final Future<?> futureProxy = executor.submit(() -> {
            try {
                sendResponse(session, clusterClients.get(nodeId).invoke(request));
            } catch (IOException | InterruptedException | PoolException | HttpException error) {
                log.error("Sending response error: ", error);
                sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        });

        if (futureProxy.isCancelled()) {
            log.error("Proxy executor error!");
        }
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
                       @NotNull final HttpSession session) {

        try {
            if (id.isEmpty()) {
                sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            }

            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final String nodeId = nodes.primaryFor(key);

            if (!nodes.getId().equals(nodeId)) {
               forwardRequest(session, nodeId, request);
               return;
            }

            final Future<?> future = executor.submit(() -> handleRequest(request, session, key));

            if (future.isCancelled()) {
                log.error("Executor error!");
            }

        } catch (RejectedExecutionException error) {
            log.error("RejectedExecution error: ", error);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    /**
     * Request handler for input requests.
     *
     * @param request - input http-request
     * @param session - http-session
     * @param key - input key
     */
    private void handleRequest(@NotNull final Request request,
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
        } catch (NoSuchElementException message) {
            log.error("NoSuchElement in database: ", message);
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

