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
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzerGet;
import ru.mail.polis.service.alexander.marashov.analyzers.SimpleResponseAnalyzer;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    private static final String ROW_ACCESS_HEADER = "Row-Access";
    private static final String WAITING_INTERRUPTED = "Responses waiting was interrupted";

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
        this.defaultAck = this.topology.size() / 2 + 1;
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

    private boolean areParametersWrong(
            final String id,
            final int ack,
            final int from
    ) {
        return id.isEmpty() || ack <= 0 || ack > from || from > topology.size();
    }

    private static int[] unpackReplicasParameter(final String replicas) throws NumberFormatException {
        if (replicas == null) {
            return new int[0];
        }
        final List<String> parameters = Splitter.on('/').splitToList(replicas);
        if (parameters.size() != 2) {
            return new int[0];
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

    /**
     * Method that puts entity to the DAO and returns response with operation results.
     * @param key - ByteBuffer that contains the key data.
     * @param value - ByteBuffer that contains the value data.
     * @return response to send.
     */
    public Response executeLocalPut(final ByteBuffer key, final ByteBuffer value) {
        try {
            this.dao.upsert(key, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (final IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Method that deletes entity from the DAO and returns response with operation results.
     * @param key - ByteBuffer that contains the key data.
     * @return response to send.
     */
    public Response executeLocalDelete(final ByteBuffer key) {
        try {
            this.dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (final IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Method that gets entity from the DAO and returns response with operation results.
     * @param key - ByteBuffer that contains key data.
     * @return response to send.
     */
    public Response executeLocalRowGet(final ByteBuffer key) {
        Value value = null;
        try {
            value = this.dao.rowGet(key);
        } catch (final IOException e) {
            log.debug("Key not found", e);
        }

        Response response;
        if (value == null) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            try {
                final byte[] serializedData = ValueSerializer.serialize(value);
                response = new Response(Response.OK, serializedData);
            } catch (IOException e) {
                response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                log.error("Local get SERIALIZE ERROR");
            }
        }
        return response;
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
                    final ValidatedParameters validatedParameters;
                    try {
                        validatedParameters = validateParameters(id, replicas);
                    } catch (final IllegalArgumentException e) {
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final String rowAccessHeader = request.getHeader(ROW_ACCESS_HEADER);

                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_GET))) {
                        sendAnswerOrError(httpSession, executeLocalRowGet(validatedParameters.key));
                        return;
                    }

                    final String[] primaries = topology.primariesFor(validatedParameters.key, validatedParameters.from);
                    final ResponseAnalyzerGet valueAnalyzer =
                            new ResponseAnalyzerGet(validatedParameters.ack, validatedParameters.from);

                    request.addHeader(ROW_ACCESS_HEADER + Request.METHOD_GET);
                    for (final String primary : primaries) {

                        if (topology.isLocal(primary)) {
                            executorService.execute(() -> {
                                Value value = null;
                                try {
                                    value = this.dao.rowGet(validatedParameters.key);
                                } catch (final NoSuchElementException | IOException e) {
                                    log.debug("Key not found", e);
                                }
                                valueAnalyzer.accept(value);
                            });

                        } else {
                            executorService.execute(() -> {
                                Response response;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
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
                        log.error(WAITING_INTERRUPTED, e);
                        Thread.currentThread().interrupt();
                    }

                    final Response response = valueAnalyzer.getResult();
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
                    final ValidatedParameters validatedParameters;
                    try {
                        validatedParameters = validateParameters(id, replicas);
                    } catch (final IllegalArgumentException e) {
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }

                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);

                    final String rowAccessHeader = request.getHeader(ROW_ACCESS_HEADER);
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_PUT))) {
                        sendAnswerOrError(httpSession, executeLocalPut(validatedParameters.key, value));
                        return;
                    }

                    final String[] primaries = topology.primariesFor(validatedParameters.key, validatedParameters.from);
                    final ResponseAnalyzer<Boolean> responseAnalyzer = new SimpleResponseAnalyzer(
                            validatedParameters.ack,
                            validatedParameters.from,
                            201,
                            Response.CREATED
                    );

                    request.addHeader(ROW_ACCESS_HEADER + Request.METHOD_PUT);
                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            executorService.execute(() -> {
                                final Response response = executeLocalPut(validatedParameters.key, value);
                                responseAnalyzer.accept(response);
                            });
                        } else {
                            executorService.execute(() -> {
                                Response response = null;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
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
                        log.error(WAITING_INTERRUPTED);
                        Thread.currentThread().interrupt();
                    }
                    final Response response = responseAnalyzer.getResult();
                    sendAnswerOrError(httpSession, response);
                }
        );
    }

    private static class ValidatedParameters {
        final int ack;
        final int from;
        final ByteBuffer key;

        public ValidatedParameters(final int ack, final int from, final ByteBuffer key) {
            this.ack = ack;
            this.from = from;
            this.key = key;
        }
    }

    /**
     * Validates parameters and throws exceptions if parameters are invalid.
     * @param id - id parameter.
     * @param replicas - replicas parameter, has ack/from format.
     * @throws IllegalArgumentException if any parameter has invalid value.
     * @return validated parameters.
     */
    private ValidatedParameters validateParameters(
            final String id,
            final String replicas
    ) throws IllegalArgumentException {
        final int[] replicasParameters = unpackReplicasParameter(replicas);
        final int ack;
        final int from;
        if (replicasParameters.length == 0) {
            ack = defaultAck;
            from = defaultFrom;
        } else {
            ack = replicasParameters[0];
            from = replicasParameters[1];
        }

        if (areParametersWrong(id, ack, from)) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer key = ByteBuffer.wrap(bytes);

        return new ValidatedParameters(ack, from, key);
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
                    final ValidatedParameters validatedParameters;
                    try {
                        validatedParameters = validateParameters(id, replicas);
                    } catch (final IllegalArgumentException e) {
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }

                    final String rowAccessHeader = request.getHeader(ROW_ACCESS_HEADER);
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_DELETE))) {
                        sendAnswerOrError(httpSession, executeLocalDelete(validatedParameters.key));
                        return;
                    }

                    final String[] primaries = topology.primariesFor(validatedParameters.key, validatedParameters.from);
                    final ResponseAnalyzer<Boolean> responseAnalyzer = new SimpleResponseAnalyzer(
                            validatedParameters.ack,
                            validatedParameters.from,
                            202,
                            Response.ACCEPTED
                    );

                    request.addHeader(ROW_ACCESS_HEADER + Request.METHOD_DELETE);
                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            executorService.execute(() -> {
                                final Response response = executeLocalDelete(validatedParameters.key);
                                responseAnalyzer.accept(response);
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
                        log.error(WAITING_INTERRUPTED);
                        Thread.currentThread().interrupt();
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
