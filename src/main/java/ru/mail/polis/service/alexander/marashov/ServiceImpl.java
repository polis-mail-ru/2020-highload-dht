package ru.mail.polis.service.alexander.marashov;

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
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

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

public class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String RESPONSE_ERROR_STRING = "Sending response error";

    private final DAO dao;
    private final ExecutorService executorService;

    private final Topology<String> topology;
    private final Map<String, HttpClient> nodeToClient;

    /**
     * Implementation of a persistent storage with HTTP API.
     *
     * @author Marashov Alexander
     */
    public ServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final Topology<String> topology,
            final int workersCount,
            final int queueSize,
            final int proxyTimeoutValue
    ) throws IOException {
        super(configFrom(port));
        this.dao = dao;
        this.executorService = new ThreadPoolExecutor(
                workersCount,
                workersCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("{}: uncaught exception", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.topology = topology;
        this.nodeToClient = new HashMap<>();
        for (final String node : this.topology.all()) {
            if (this.topology.isLocal(node)) {
                return;
            }
            final HttpClient httpClient = new HttpClient(new ConnectionString(node + "?timeout=" + proxyTimeoutValue));
            if (nodeToClient.put(node, httpClient) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }
    }

    /**
     * Static function for creating an {@link HttpServerConfig} instance.
     *
     * @param port port on which the {@link HttpServer} should listen.
     * @return {@link HttpServerConfig} for {@link HttpServer} with specified port.
     */
    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    /**
     * Static function for converting {@link ByteBuffer} object to simple byte[] array.
     *
     * @param buffer {@link ByteBuffer} instance that needs to be converted.
     * @return byte[] array with buffer's data or empty array if buffer is empty.
     */
    @NotNull
    private static byte[] getBytes(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private void executeOrSendError(final HttpSession httpSession, final Runnable runnable) {
        try {
            executorService.execute(runnable);
        } catch (final RejectedExecutionException e) {
            log.error("Request rejected", e);
            sendAnswerOrError(httpSession, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    private void sendAnswerOrError(final HttpSession httpSession, final Response response) {
        try {
            httpSession.sendResponse(response);
        } catch (final IOException ioException) {
            log.error(RESPONSE_ERROR_STRING, ioException);
        }
    }

    private boolean sendErrorOnWrongId(final String id, final HttpSession sessionToSendError) {
        final boolean isEmpty = id.isEmpty();
        if (isEmpty) {
            sendAnswerOrError(sessionToSendError, new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
        return isEmpty;
    }

    private boolean proxyIfNotLocal(
            final ByteBuffer key,
            final HttpSession session,
            final Request request
    ) {
        final String node = topology.primaryFor(key);
        final boolean notLocal = !topology.isLocal(node);
        if (notLocal) {
            try {
                final Response response = nodeToClient.get(node).invoke(request);
                sendAnswerOrError(session, response);
            } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                log.error("Can't proxy request", e);
                sendAnswerOrError(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        }
        return notLocal;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    /**
     * Http method handler for checking server's reachability.
     *
     * @return {@link Response} with status {@code 200} if the server is available.
     */
    @Path("/v0/status")
    public Response handleStatus() {
        return new Response(Response.OK, Response.EMPTY);
    }

    /**
     * Http method handler for getting a value in the DAO by the key.
     *
     * @param id is the key for searching for a value in the DAO.
     *           Sends {@link Response} instance with value as body, if the key exists. Response status is
     * {@code 200} if data is found
     * {@code 400} if id is empty
     * {@code 404} if not found,
     * {@code 500} if an internal server error occurred.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleEntityGet(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    if (sendErrorOnWrongId(id, httpSession)) {
                        return;
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    if (proxyIfNotLocal(key, httpSession, request)) {
                        return;
                    }

                    final ByteBuffer result;
                    try {
                        result = this.dao.get(key);
                    } catch (final NoSuchElementException e) {
                        sendAnswerOrError(httpSession, new Response(Response.NOT_FOUND, Response.EMPTY));
                        return;
                    } catch (final IOException e) {
                        log.error("Get entity method: key = '{}' error", id, e);
                        sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                        return;
                    }
                    sendAnswerOrError(httpSession, new Response(Response.OK, getBytes(result)));
                }
        );
    }

    /**
     * HTTP method handler for placing a value by the key in the DAO storage.
     *
     * @param id is the key that the data will be associated with.
     *           Sends {@link Response} instance with
     * {@code 201} if data saved
     * {@code 400} if id is empty,
     * {@code 500} if an internal server error occurred.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void handleEntityPut(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    if (sendErrorOnWrongId(id, httpSession)) {
                        return;
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    if (proxyIfNotLocal(key, httpSession, request)) {
                        return;
                    }

                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);

                    try {
                        this.dao.upsert(key, value);
                    } catch (final IOException e) {
                        log.error("Put entity method: key = '{}', value = '{}' error", id, body.length, e);
                        sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                        return;
                    }
                    sendAnswerOrError(httpSession, new Response(Response.CREATED, Response.EMPTY));
                }
        );
    }

    /**
     * HTTP method handler for removing a value by the key from the DAO storage.
     *
     * @param id is the key that the data associated with.
     *           Sends {@link Response} instance with
     * {@code 202} if the key deleted,
     * {@code 400} if id is empty,
     * {@code 500} if an internal server error occurred.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void handleEntityDelete(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    if (sendErrorOnWrongId(id, httpSession)) {
                        return;
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    if (proxyIfNotLocal(key, httpSession, request)) {
                        return;
                    }

                    try {
                        this.dao.remove(key);
                    } catch (final IOException e) {
                        log.error("Delete entity method: key = '{}' error", id, e);
                        sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                        return;
                    }
                    sendAnswerOrError(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
                }
        );
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            log.error("Waiting for a stop is interrupted");
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client: nodeToClient.values()) {
            client.clear();
        }
    }
}
