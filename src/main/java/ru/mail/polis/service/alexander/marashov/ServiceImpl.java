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
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceImpl extends HttpServer implements Service {

    private final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final DAO dao;
    private final ExecutorService executorService;
    private final static String responseErrorString = "Sending response error";

    /**
     * Implementation of a persistent storage with HTTP API.
     *
     * @author Marashov Alexander
     */
    public ServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final int workersCount,
            final int queueSize
    ) throws IOException {
        super(configFrom(port));
        BasicConfigurator.configure();
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
            try {
                httpSession.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (final IOException ioException) {
                log.error(responseErrorString, ioException);
            }
        }
    }

    private void trySendError(final HttpSession httpSession, final Response response) {
        try {
            httpSession.sendResponse(response);
        } catch (final IOException ioException) {
            log.error(responseErrorString, ioException);
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
     * {@code 200} if data is found
     * {@code 400} if id is empty
     * {@code 404} if not found,
     * {@code 500} if an internal server error occurred.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void handleEntityGet(final HttpSession httpSession, @Param(value = "id", required = true) final String id) {
        executeOrSendError(
                httpSession,
                () -> {
                    if (id.isEmpty()) {
                        log.debug("Get entity method: key is empty");
                        trySendError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                        return;
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    final ByteBuffer result;
                    try {
                        result = this.dao.get(key);
                    } catch (final NoSuchElementException e) {
                        log.debug(String.format("Get entity method: key = '%s' not found", id));
                        trySendError(httpSession, new Response(Response.NOT_FOUND, Response.EMPTY));
                        return;
                    } catch (final IOException e) {
                        log.error(String.format("Get entity method: key = '%s' error", id), e);
                        trySendError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                        return;
                    }
                    trySendError(httpSession, new Response(Response.OK, getBytes(result)));
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
                    if (id.isEmpty()) {
                        log.debug("Put entity method: key is empty");
                        trySendError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    }

                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);

                    final byte[] body = request.getBody();
                    final ByteBuffer value = ByteBuffer.wrap(body);

                    try {
                        this.dao.upsert(key, value);
                    } catch (IOException e) {
                        log.error(
                                String.format(
                                        "Put entity method: key = '%s', value = '%s' error",
                                        id,
                                        Arrays.toString(body)
                                ),
                                e
                        );
                        trySendError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    }
                    trySendError(httpSession, new Response(Response.CREATED, Response.EMPTY));
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
            @Param(value = "id", required = true) final String id
    ) {
        executeOrSendError(
                httpSession,
                () -> {
                    if (id.isEmpty()) {
                        log.debug("Delete entity method: key is empty");
                        trySendError(httpSession, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    }
                    final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
                    final ByteBuffer key = ByteBuffer.wrap(bytes);
                    try {
                        this.dao.remove(key);
                    } catch (IOException e) {
                        log.error(String.format("Delete entity method: key = '%s' error", id), e);
                        trySendError(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    }
                    trySendError(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
                }
        );
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Waiting for a stop is interrupted");
        }
    }
}
