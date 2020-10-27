package ru.mail.polis.service.alexander.marashov;

import com.google.common.base.Splitter;
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
import ru.mail.polis.dao.alexander.marashov.Value;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzer;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzerDelete;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzerGet;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzerPut;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

public class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String RESPONSE_ERROR_STRING = "Sending response error";

    private final DAO dao;
    private final ExecutorService executorService;

    private final Topology<String> topology;
    private final Map<String, HttpClient> nodeToClient;

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
        this.defaultAck = (this.topology.size() / 2) + 1;
        this.defaultFrom = this.topology.size();

        this.nodeToClient = new HashMap<>();
        for (final String node : this.topology.all()) {
            if (this.topology.isLocal(node)) {
                continue;
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

    private boolean sendErrorIfWrongParameters(
            final String id,
            final int ack,
            final int from,
            final HttpSession sessionToSendError
    ) {
        final boolean wrongParameters = id.isEmpty() || ack <= 0 || ack > from || from > topology.size();
        if (wrongParameters) {
            sendAnswerOrError(sessionToSendError, new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
        return wrongParameters;
    }

    private static int[] unpackReplicasParameter(final String replicas) throws NumberFormatException {
        if (replicas == null) {
            return null;
        }
        final List<String> parameters = Splitter.on('/').splitToList(replicas);
        if (parameters.size() != 2) {
            return null;
        }

        final int ack = Integer.parseInt(parameters.get(0));
        final int from = Integer.parseInt(parameters.get(1));
        return new int[]{ack, from};
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    public void executeLocalPut(
            final HttpSession httpSession,
            final ByteBuffer key,
            final ByteBuffer value
    ) {
        try {
            this.dao.upsert(key, value);
            sendAnswerOrError(httpSession, new Response(Response.CREATED, Response.EMPTY));
        } catch (final IOException e) {
            sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    public void executeLocalDelete(
            final HttpSession httpSession,
            final ByteBuffer key
    ) {
        try {
            this.dao.remove(key);
            sendAnswerOrError(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (final IOException e) {
            sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    public void executeLocalRowGet(
            final HttpSession httpSession,
            final ByteBuffer key
    ) {
        log.debug("Handle local row get");
        Value value = null;
        try {
            value = this.dao.rowGet(key);
            log.debug("Local get without errors");
        } catch (final IOException e) {
            log.debug("Key not found", e);
        }

        Response response;
        if (value == null) {
            log.debug("Local get NOT FOUND");
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            try {
                log.debug("Local get FOUND");
                final byte[] serializedData = ValueSerializer.serialize(value);
                response = new Response(Response.OK, serializedData);
                log.debug("Local get OK");
            } catch (IOException e) {
                response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                log.error("Local get SERIALIZE ERROR");
            }
        }
        sendAnswerOrError(httpSession, response);
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
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    log.debug("get");

                    final int[] replicasParameters;
                    try {
                        replicasParameters = unpackReplicasParameter(replicas);
                    } catch (final NumberFormatException e) {
                        log.error("Invalid replicas parameter format");
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }

                    final int ack;
                    final int from;
                    if (replicasParameters == null) {
                        ack = defaultAck;
                        from = defaultFrom;
                    } else {
                        ack = replicasParameters[0];
                        from = replicasParameters[1];
                    }

                    if (sendErrorIfWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);

                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_GET))) {
                        log.debug("get was proxied");
                        executeLocalRowGet(httpSession, key);
                        return;
                    }
                    log.debug("get was NOT proxied");

                    final String[] primaries = topology.primariesFor(key, from);
                    final ResponseAnalyzerGet valueAnalyzer = new ResponseAnalyzerGet(ack, from);

                    log.debug("GET needs ack = {}, from = {}, primaries = {}",
                            ack, from, Arrays.toString(primaries));

                    request.addHeader("Row-Access" + Request.METHOD_GET);
                    for (final String primary : primaries) {

                        if (topology.isLocal(primary)) {

                            executorService.execute(() -> {
                                log.debug("get, local execution");
                                Value value = null;
                                try {
                                    value = this.dao.rowGet(key);
                                    log.debug("get, local execution, rowGet successful");
                                } catch (final NoSuchElementException | IOException e) {
                                    log.debug("Key not found", e);
                                }
                                valueAnalyzer.accept(value);
                            });

                        } else {
                            log.debug("get, primary = {} is NOT local", primary);
                            executorService.execute(() -> {

                                Response response;
                                try {
                                    log.debug("get, proxy to another node");
                                    response = nodeToClient.get(primary).invoke(request);
                                    log.debug("get, proxy success");
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    response = null;
                                    log.error("Get: Error sending request to node {}", primary, e);
                                }
                                valueAnalyzer.accept(response);
                            });
                        }
                    }

                    try {
                        valueAnalyzer.await(1000L, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted", e);
                    }

                    final Response response = valueAnalyzer.getResult();
                    log.debug("Get: response status = {}", response.getStatus());
                    sendAnswerOrError(httpSession, response);
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
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    final int[] replicasParameters;
                    try {
                        replicasParameters = unpackReplicasParameter(replicas);
                    } catch (final NumberFormatException e) {
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }

                    final int ack;
                    final int from;
                    if (replicasParameters == null) {
                        ack = defaultAck;
                        from = defaultFrom;
                    } else {
                        ack = replicasParameters[0];
                        from = replicasParameters[1];
                    }

                    if (sendErrorIfWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);

                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_PUT))) {
                        log.debug("put was proxied");
                        executeLocalPut(httpSession, key, value);
                        return;
                    }
                    log.debug("put was not proxied");


                    final String[] primaries = topology.primariesFor(key, from);

                    log.debug("PUT needs ack = {}, from = {}, primaries = {}",
                            ack, from, Arrays.toString(primaries));

                    final ResponseAnalyzer<Boolean> responseAnalyzer = new ResponseAnalyzerPut(ack, from);

                    request.addHeader("Row-Access" + Request.METHOD_PUT);
                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            log.debug("put, primary = {} is local", primary);
                            executorService.execute(() -> {
                                log.debug("put, local execution");
                                try {
                                    this.dao.upsert(key, value);
                                    log.debug("put, local exe, OK");
                                    responseAnalyzer.accept(true);
                                } catch (final IOException e) {
                                    log.error("Upsert method error", e);
                                    responseAnalyzer.accept(false);
                                }
                            });
                        } else {
                            log.debug("put, primary = {} is NOT local", primary);
                            executorService.execute(() -> {
                                Response response = null;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
                                    log.debug("put, proxy answer has successful result");
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    log.error("Upsert: Error sending request to node {}", primary, e);
                                }
                                responseAnalyzer.accept(response);
                            });
                        }
                    }

                    try {
                        responseAnalyzer.await(1000, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted");
                    }
                    final Response response = responseAnalyzer.getResult();
                    log.debug("put, response ready, status = {}", response.getStatus());
                    sendAnswerOrError(httpSession, response);
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
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    final int[] replicasParameters;
                    try {
                        replicasParameters = unpackReplicasParameter(replicas);
                    } catch (final NumberFormatException e) {
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final int ack;
                    final int from;
                    if (replicasParameters == null) {
                        ack = defaultAck;
                        from = defaultFrom;
                    } else {
                        ack = replicasParameters[0];
                        from = replicasParameters[1];
                    }

                    if (sendErrorIfWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);

                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_DELETE))) {
                        executeLocalDelete(httpSession, key);
                        return;
                    }

                    final String[] primaries = topology.primariesFor(key, from);
                    final ResponseAnalyzer<Boolean> responseAnalyzer = new ResponseAnalyzerDelete(ack, from);

                    request.addHeader("Row-Access" + Request.METHOD_DELETE);
                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            executorService.execute(() -> {
                                try {
                                    this.dao.remove(key);
                                    responseAnalyzer.accept(true);
                                } catch (final IOException e) {
                                    log.error("Delete entity method error", e);
                                    responseAnalyzer.accept(false);
                                }
                            });
                        } else {
                            executorService.execute(() -> {
                                Response response = null;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    log.error("Delete: Error sending request to node {}", primary, e);
                                }
                                responseAnalyzer.accept(response);
                            });
                        }
                    }


                    try {
                        responseAnalyzer.await(1000, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted");
                    }

                    final Response response = responseAnalyzer.getResult();
                    sendAnswerOrError(httpSession, response);
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
        try {
            dao.close();
        } catch (final IOException e) {
            log.error("Error closing dao", e);
        }
    }
}
