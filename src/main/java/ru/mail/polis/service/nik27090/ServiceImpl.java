package ru.mail.polis.service.nik27090;

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

import javax.mail.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    @NotNull
    private final ExecutorService executorService;

    @NotNull
    private final DAO dao;

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
            final int queueCapacity) throws IOException {
        super(createConfig(port));
        this.dao = dao;
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
     * Get data by key.
     *
     * @param id      - key for storage
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void getEntity(
            final @Param(value = "id", required = true) String id,
            final HttpSession session) {
        log.debug("GET request: id = {}", id);

        try {
            executorService.execute(() -> {
                if (id.isEmpty()) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                try {
                    final ByteBuffer value = dao.get(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8))).duplicate();
                    final byte[] response = new byte[value.remaining()];
                    value.get(response);
                    sendResponse(session, Response.ok(response));
                } catch (NoSuchElementException e) {
                    sendResponse(session, new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Internal error with id = {}", id, e);
                    sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Executor has been shut down or" +
                    "executor uses finite bounds for both maximum threads and work queue capacity", e);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE));
        }
    }

    /**
     * Create/overwrite data by key.
     *
     * @param id      - key for storage
     * @param request - body request
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void putEntity(
            @Param(value = "id", required = true) final String id,
            final Request request,
            final HttpSession session) {
        log.debug("PUT request: id = {}, value length = {}", id, request.getBody().length);
        try {
            executorService.execute(() -> {
                if (id.isEmpty()) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                try {
                    final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                    final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                    dao.upsert(key, value);
                    sendResponse(session, new Response(Response.CREATED, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Internal error with id = {}, value length = {}", id, request.getBody().length, e);
                    sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Executor has been shut down or" +
                    "executor uses finite bounds for both maximum threads and work queue capacity", e);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE));
        }
    }

    /**
     * Delete data by key.
     *
     * @param id      - key for storage
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void deleteEntity(@Param(value = "id", required = true) final String id, final HttpSession session) {
        log.debug("DELETE request: id = {}", id);
        try {
            executorService.execute(() -> {
                if (id.isEmpty()) {
                    sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                try {
                    dao.remove(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
                    sendResponse(session, new Response(Response.ACCEPTED, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Internal error with id = {}", id, e);
                    sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Executor has been shut down or" +
                    "executor uses finite bounds for both maximum threads and work queue capacity", e);
            sendResponse(session, new Response(Response.SERVICE_UNAVAILABLE));
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
    }

    private void sendResponse(@NotNull final HttpSession session, @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error("Can't send response", e);
        }
    }
}
