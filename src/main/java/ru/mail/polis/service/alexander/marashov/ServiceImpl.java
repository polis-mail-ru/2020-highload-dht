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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
                        .setUncaughtExceptionHandler((t, e) -> log.error("uncaught exception", t, e))
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

//    /**
//     * Static function for converting {@link ByteBuffer} object to simple byte[] array.
//     *
//     * @param buffer {@link ByteBuffer} instance that needs to be converted.
//     * @return byte[] array with buffer's data or empty array if buffer is empty.
//     */
//    @NotNull
//    private static byte[] getBytes(@NotNull final ByteBuffer buffer) {
//        if (!buffer.hasRemaining()) {
//            return Response.EMPTY;
//        }
//        final byte[] result = new byte[buffer.remaining()];
//        buffer.get(result);
//        return result;
//    }

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

    private boolean sendErrorOnWrongParameters(
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

//    private boolean proxyIfNotLocal(
//            final ByteBuffer key,
//            final HttpSession session,
//            final Request request
//    ) {
//        final String node = topology.primaryFor(key);
//        final boolean notLocal = !topology.isLocal(node);
//        if (notLocal) {
//            try {
//                final Response response = nodeToClient.get(node).invoke(request);
//                sendAnswerOrError(session, response);
//            } catch (final InterruptedException | PoolException | IOException | HttpException e) {
//                log.error("Can't proxy request", e);
//                sendAnswerOrError(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
//            }
//        }
//        return notLocal;
//    }

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

    public void handlePut(
            final HttpSession httpSession,
            final ByteBuffer key,
            final ByteBuffer value
    ) {
        log.debug("Handle local put");
        try {
            this.dao.upsert(key, value);
            log.debug("Local put OK");
            sendAnswerOrError(httpSession, new Response(Response.CREATED, Response.EMPTY));
        } catch (final IOException e) {
            log.error("Dao upsert error", e);
            sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    public void handleDelete(
            final HttpSession httpSession,
            final ByteBuffer key
    ) {
        log.debug("Handle local delete");
        try {
            this.dao.remove(key);
            log.debug("Local delete OK");
        } catch (final IOException e) {
            log.error("Delete entity method: key = '{}' error", key, e);
            sendAnswerOrError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }
        sendAnswerOrError(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
    }

    public void handleRowGet(
            final HttpSession httpSession,
            final ByteBuffer key
    ) {
        log.debug("Handle local row get");
        Value value = null;
        try {
            value = this.dao.rowGet(key);
            log.debug("Local get without errors");
        } catch (final IOException e) {
            log.debug("Key {} not found", key, e);
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
                    final int ack = replicasParameters == null ? defaultAck : replicasParameters[0];
                    final int from = replicasParameters == null ? defaultFrom : replicasParameters[1];
                    if (sendErrorOnWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);

                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_GET))) {
                        log.debug("get is proxied to me!");
                        handleRowGet(httpSession, key);
                        return;
                    }
                    log.debug("get was not proxied");

                    final String[] primaries = topology.primariesFor(key, from);

                    log.debug("GET needs ack = {}, from = {}, primaries = {}",
                             ack, from, Arrays.toString(primaries));

                    final ResponseAnalyzer valueAnalyzer = new ResponseAnalyzer(ack);
                    final Lock lock = new ReentrantLock();
                    final Condition condition = lock.newCondition();
                    request.addHeader("Row-Access" + Request.METHOD_GET);

                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            log.debug("get, primary = {} is local", primary);
                            executorService.execute(() -> {
                                log.debug("get, local execution");
                                Value value = null;
                                try {
                                    value = this.dao.rowGet(key);
                                    log.debug("get, local execution, rowGet successful");
                                } catch (final NoSuchElementException | IOException e) {
                                    log.debug("get, lcl execution, Key {} not found", key, e);
                                }
                                if (valueAnalyzer.accept(value)) {
                                    log.debug("get, lcl exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("get, lcl exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("get, lcl exe, unlocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        } else {
                            log.debug("get, primary = {} is NOT local", primary);
                            executorService.execute(() -> {

                                Response response;
                                try {
                                    log.debug("get, proxy to another node");
                                    response = nodeToClient.get(primary).invoke(request);
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    response = null;
                                    log.error("get, Invoke response error", e);
                                }
                                if (valueAnalyzer.accept(response)) {
                                    log.debug("get, from proxy exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("get, from proxy exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("get, from proxy exe, unlocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        }
                    }

                    log.debug("get, after cycle");
                    lock.lock();
                    try {
                        log.debug("get, after cycle, locked");
                        while (!valueAnalyzer.hasEnoughAnswers()) {
                            log.debug("get, after cycle, locked, awaiting condition");
                            final boolean timeIsOut = !condition.await(1000, TimeUnit.MILLISECONDS);
                            if (timeIsOut) {
                                log.debug("get, after cycle, locked, time is OUT");
                                break;
                            }
                        }
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted");
                    } finally {
                        log.debug("get, after cycle, UNlocked");
                        lock.unlock();
                    }

                    log.debug("get, answers = {}, need = {}, hasEnough = {}",

                            valueAnalyzer.answeredCount,
                            ack,
                            valueAnalyzer.hasEnoughAnswers());

                    final Value correctValue = valueAnalyzer.getCorrectValue();
                    Response response;
                    if (correctValue == null) {
                        log.debug("get, no enough answers, GATEWAY_TIMEOUT");
                        response = new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
                    } else if (correctValue.isTombstone()) {
                        log.debug("get, no enough answers, NOT FOUND");
                        response = new Response(Response.NOT_FOUND, Response.EMPTY);
                    } else {
                        log.debug("get, no enough answers, OK");
                        response = new Response(Response.OK, correctValue.getData().array());
                    }
                    sendAnswerOrError(httpSession, response);
                }
        );
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
                        log.error("Invalid replicas parameter format");
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final int ack = replicasParameters == null ? defaultAck : replicasParameters[0];
                    final int from = replicasParameters == null ? defaultFrom : replicasParameters[1];

                    log.debug("put");

                    if (sendErrorOnWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);

                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_PUT))) {
                        log.debug("put is proxied to me!");
                        handlePut(httpSession, key, value);
                        return;
                    }
                    log.debug("put was not proxied");


                    final String[] primaries = topology.primariesFor(key, from);
                    log.debug("PUT needs ack = {}, from = {}, primaries = {}",
                             ack, from, Arrays.toString(primaries));

                    final AtomicInteger successAnswers = new AtomicInteger(0);
                    final AtomicInteger totalAnswers = new AtomicInteger(0);

                    final Lock lock = new ReentrantLock();
                    final Condition condition = lock.newCondition();

                    request.addHeader("Row-Access" + Request.METHOD_PUT);

                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            log.debug("put, primary = {} is local", primary);
                            executorService.execute(() -> {
                                log.debug("put, local execution");
                                try {
                                    this.dao.upsert(key, value);
                                    log.debug("put, local execution, upsert successful");
                                    successAnswers.incrementAndGet();
                                } catch (final IOException e) {
                                    log.error("Put entity method: key = '{}', value = '{}' error"
                                            , id, body.length, e);
                                }
                                if (totalAnswers.incrementAndGet() == from || successAnswers.get() >= ack) {
                                    log.debug("put, lcl exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("put, lcl exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("put, lcl exe, unlocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        } else {
                            log.debug("put, primary = {} is NOT local", primary);
                            executorService.execute(() -> {
                                log.debug("put, proxy to another node");
                                Response response;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
                                    if (response.getStatus() == 201) {
                                        log.debug("put, proxy answer has successful result");
                                        successAnswers.incrementAndGet();
                                    }
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    log.error("put, Invoke response error", e);
                                }
                                if (totalAnswers.incrementAndGet() == from || successAnswers.get() >= ack) {
                                    log.debug("put, from proxy exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("put, from proxy exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("put, from proxy exe, UNlocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        }
                    }

                    log.debug("put, after cycle");
                    lock.lock();
                    try {
                        log.debug("put, after cycle, locked");
                        while (successAnswers.get() < ack) {
                            log.debug("put, after cycle, locked, awaiting condition");
                            final boolean timeIsOut = !condition.await(1000, TimeUnit.MILLISECONDS);
                            if (timeIsOut) {
                                log.debug("put, after cycle, locked, time is OUT");
                                break;
                            }
                        }
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted");
                    } finally {
                        log.debug("put, after cycle, UNLocked");
                        lock.unlock();
                    }

                    log.debug("put, success answers = {}, answers = {}, need = {}",
                            successAnswers,
                            totalAnswers,
                            ack);

                    if (successAnswers.get() >= ack) {
                        log.debug("put, has enough answers, CREATED");
                        sendAnswerOrError(httpSession, new Response(Response.CREATED, Response.EMPTY));
                    } else {
                        log.debug("put, has not enough answers, GATEWAY_TIMEOUT");
                        sendAnswerOrError(httpSession, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    }
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
                    log.debug("delete");

                    final int[] replicasParameters;
                    try {
                        replicasParameters = unpackReplicasParameter(replicas);
                    } catch (final NumberFormatException e) {
                        log.error("Invalid replicas parameter format");
                        sendAnswerOrError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final int ack = replicasParameters == null ? defaultAck : replicasParameters[0];
                    final int from = replicasParameters == null ? defaultFrom : replicasParameters[1];
                    if (sendErrorOnWrongParameters(id, ack, from, httpSession)) {
                        return;
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);


                    final String rowAccessHeader = request.getHeader("Row-Access");
                    if (rowAccessHeader != null && rowAccessHeader.equals(Integer.toString(Request.METHOD_DELETE))) {
                        log.debug("delete is proxied to me!");
                        handleDelete(httpSession, key);
                        return;
                    }
                    log.debug("delete was not proxied");

                    final String[] primaries = topology.primariesFor(key, from);

                    log.debug("DELETE needs ack = {}, from = {}, primaries = {}",
                            ack, from, Arrays.toString(primaries));

                    final AtomicInteger successAnswers = new AtomicInteger(0);
                    final AtomicInteger totalAnswers = new AtomicInteger(0);
                    final Lock lock = new ReentrantLock();
                    final Condition condition = lock.newCondition();

                    request.addHeader("Row-Access" + Request.METHOD_DELETE);

                    for (final String primary : primaries) {
                        if (topology.isLocal(primary)) {
                            log.debug("delete, primary = {} is local", primary);
                            executorService.execute(() -> {
                                log.debug("delete, local execution");
                                try {
                                    this.dao.remove(key);
                                    successAnswers.incrementAndGet();
                                    log.debug("delete, local execution, delete successful");
                                } catch (final IOException e) {
                                    log.error("Delete entity method: key = '{}' error", id, e);
                                }
                                if (totalAnswers.incrementAndGet() == from || successAnswers.get() >= ack) {
                                    log.debug("Delete, lcl exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("Delete, lcl exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("Delete, lcl exe, UNLocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        } else {
                            log.debug("delete, primary = {} is NOT local", primary);
                            executorService.execute(() -> {
                                log.debug("DELETE, proxy to another node");
                                Response response;
                                try {
                                    response = nodeToClient.get(primary).invoke(request);
                                    if (response.getStatus() == 202) {
                                        log.debug("delete, proxy answer has successful result");
                                        successAnswers.incrementAndGet();
                                    }
                                } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                                    log.error("delete, Node invoke error", e);
                                }
                                if (totalAnswers.incrementAndGet() == from || successAnswers.get() >= ack) {
                                    log.debug("delete, from proxy exe, has enough answers");
                                    lock.lock();
                                    try {
                                        log.debug("delete, from proxy exe, locked");
                                        condition.signalAll();
                                    } finally {
                                        log.debug("delete, from proxy exe, UNLocked");
                                        lock.unlock();
                                    }
                                }
                            });
                        }
                    }

                    log.debug("delete, after cycle");
                    lock.lock();
                    try {
                        log.debug("delete, after cycle, locked");
                        while (successAnswers.get() < ack) {
                            log.debug("delete, after cycle, locked, awaiting condition");
                            final boolean timeIsOut = !condition.await(1000, TimeUnit.MILLISECONDS);
                            if (timeIsOut) {
                                log.debug("delete, after cycle, locked, time is OUT");
                                break;
                            }
                        }
                    } catch (final InterruptedException e) {
                        log.error("Responses waiting was interrupted");
                    } finally {
                        log.debug("delete, after cycle, UNLocked");
                        lock.unlock();
                    }

                    log.debug("delete, success answers = {}, answers = {}, need = {}",
                            successAnswers,
                            totalAnswers,
                            ack);

                    if (successAnswers.get() >= ack) {
                        log.debug("delete, has enough answers, ACCEPTED");
                        sendAnswerOrError(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
                    } else {
                        log.debug("delete, has not enough answers, GATEWAY_TIMEOUT");
                        sendAnswerOrError(httpSession, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    }
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
