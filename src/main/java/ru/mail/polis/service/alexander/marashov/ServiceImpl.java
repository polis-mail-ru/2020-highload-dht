package ru.mail.polis.service.alexander.marashov;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.service.alexander.marashov.ValidatedParameters.validateParameters;

public class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public static final String PROXY_HEADER = "Proxy-Header";
    public static final String PROXY_HEADER_VALUE = "Proxy-Header: true";

    private final ExecutorService executorService;
    private final ResponseManager responseManager;

    private final int nodesCount;
    private final int defaultAck;
    private final int defaultFrom;

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
        this.responseManager = new ResponseManager(dao, topology, proxyTimeoutValue, executorService);
        this.defaultAck = topology.getQuorumCount();
        this.defaultFrom = topology.size();
        this.nodesCount = topology.size();
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

    private void executeOrSendError(final HttpSession httpSession, final Runnable runnable) {
        try {
            executorService.execute(runnable);
        } catch (final RejectedExecutionException e) {
            log.error("Request rejected", e);
            trySendAnswer(httpSession, new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        }
    }

    private void trySendAnswer(final HttpSession httpSession, final Response response) {
        try {
            httpSession.sendResponse(response);
        } catch (final IOException ioException) {
            log.error("Sending response error", ioException);
        }
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
     *           {@code 200} if data is found
     *           {@code 400} if id is empty
     *           {@code 404} if not found,
     *           {@code 500} if an internal server error occurred.
     *           {@code 504} if not enough replicas.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleEntityGet(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    final ValidatedParameters validParams;
                    try {
                        validParams = validateParameters(id, replicas, defaultAck, defaultFrom, nodesCount);
                    } catch (final IllegalArgumentException e) {
                        trySendAnswer(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    trySendAnswer(httpSession, responseManager.get(validParams, request));
                }
        );
    }

    /**
     * HTTP method handler for placing a value by the key in the DAO storage.
     *
     * @param id is the key that the data will be associated with.
     *           Sends {@link Response} instance with
     *           {@code 201} if data saved
     *           {@code 400} if id is empty,
     *           {@code 500} if an internal server error occurred.
     *           {@code 504} if not enough replicas.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void handleEntityPut(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    final ValidatedParameters validParams;
                    try {
                        validParams = validateParameters(id, replicas, defaultAck, defaultFrom, nodesCount);
                    } catch (final IllegalArgumentException e) {
                        trySendAnswer(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);
                    trySendAnswer(httpSession, responseManager.put(validParams, value, request));
                }
        );
    }

    /**
     * HTTP method handler for removing a value by the key from the DAO storage.
     *
     * @param id is the key that the data associated with.
     *           Sends {@link Response} instance with
     *           {@code 202} if the key deleted,
     *           {@code 400} if id is empty,
     *           {@code 500} if an internal server error occurred.
     *           {@code 504} if not enough replicas.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void handleEntityDelete(
            final HttpSession httpSession,
            final Request request,
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    final ValidatedParameters validParams;
                    try {
                        validParams = validateParameters(id, replicas, defaultAck, defaultFrom, nodesCount);
                    } catch (final IllegalArgumentException e) {
                        trySendAnswer(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    trySendAnswer(httpSession, responseManager.delete(validParams, request));
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
        responseManager.clear();
    }
}
