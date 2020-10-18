package ru.mail.polis.service.bmendli;

import com.google.common.base.Charsets;
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
import ru.mail.polis.dao.bmendli.NoSuchElementLightException;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyService extends HttpServer implements Service {

    private static final String SERVER_ERROR_MSG = "Server error, cant send response for session {}";
    private static final String EXECUTING_ERROR = "Error executing the task by executor";

    private final Logger log = LoggerFactory.getLogger(MyService.class);

    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final Map<String, HttpClient> httpClients;

    /**
     * My implementation of {@link Service}.
     */
    public MyService(final int port,
                     @NotNull final DAO dao,
                     @NotNull final Topology<String> topology,
                     final int threadCount,
                     final int queueCapacity) throws IOException {
        super(createConfigFromPort(port, threadCount));
        this.dao = dao;
        this.topology = topology;
        this.httpClients = new HashMap<>();
        for (final String node : topology.all()) {
            if (topology.isLocal(node)) {
                continue;
            }
            final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
            if (httpClients.put(node, client) != null) {
                throw new IllegalStateException("Duplicate node");
            }
        }

        this.executorService = new ThreadPoolExecutor(
                threadCount,
                queueCapacity,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                        .setNameFormat("2020-highload-dht-thread-%d")
                        .setUncaughtExceptionHandler((thread, e) -> log.error("error in {} thread", thread, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Get/Put/Delete request. Return/Put/Delete a data which associated with
     * passed id in path '/v0/entity' from dao.
     */
    @Path("/v0/entity")
    public void entity(@NotNull @Param(required = true, value = "id") final String id,
                       @NotNull final HttpSession session,
                       @NotNull final Request request) {
        try {
            executorService.execute(() -> handleRequest(id, session, request));
        } catch (final RejectedExecutionException e) {
            log.error(EXECUTING_ERROR, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (final IOException ioException) {
                log.error(SERVER_ERROR_MSG, session, e);
            }
        }
    }

    /**
     * Return status for path '/v0/status'.
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        try {
            executorService.execute(() -> {
                try {
                    session.sendResponse(Response.ok(Response.EMPTY));
                } catch (final IOException e) {
                    log.error(SERVER_ERROR_MSG, session, e);
                }
            });
        } catch (final RejectedExecutionException e) {
            log.error(EXECUTING_ERROR, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (final IOException ioException) {
                log.error(SERVER_ERROR_MSG, session, e);
            }
        }
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) {
        try {
            executorService.execute(() -> {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (final IOException e) {
                    log.error(SERVER_ERROR_MSG, session, e);
                }
            });
        } catch (final RejectedExecutionException e) {
            log.error(EXECUTING_ERROR, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (final IOException ioException) {
                log.error(SERVER_ERROR_MSG, session, e);
            }
        }
    }

    private void handleRequest(@NotNull String id, @NotNull HttpSession session, @NotNull Request request) {
        try {
            if (id.isBlank()) {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            }
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    handleGet(id, session, request);
                    break;
                case Request.METHOD_PUT:
                    handlePut(id, session, request);
                    break;
                case Request.METHOD_DELETE:
                    handleDelete(id, session, request);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(SERVER_ERROR_MSG, session, e);
        }
    }

    private void handleGet(@NotNull final String id,
                           @NotNull final HttpSession session,
                           @NotNull final Request request) throws IOException {
        final byte[] bytes = id.getBytes(Charsets.UTF_8);
        final ByteBuffer wrappedBytes = ByteBuffer.wrap(bytes);
        final String node = topology.primaryFor(wrappedBytes);
        if (topology.isLocal(node)) {
            try {
                final ByteBuffer byteBuffer = dao.get(wrappedBytes);
                session.sendResponse(Response.ok(getBytesFromByteBuffer(byteBuffer)));
            } catch (NoSuchElementLightException e) {
                log.error("Does not exist record by id = {}", id, e);
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            }
        } else {
            session.sendResponse(proxy(node, request));
        }
    }

    private void handlePut(@NotNull final String id,
                           @NotNull final HttpSession session,
                           @NotNull final Request request) throws IOException {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
        final String node = topology.primaryFor(key);
        if (topology.isLocal(node)) {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } else {
            session.sendResponse(proxy(node, request));
        }
    }

    private void handleDelete(@NotNull final String id,
                              @NotNull final HttpSession session,
                              @NotNull final Request request) throws IOException {
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
        final String node = topology.primaryFor(key);
        if (topology.isLocal(node)) {
            dao.remove(key);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } else {
            session.sendResponse(proxy(node, request));
        }
    }

    @NotNull
    private static byte[] getBytesFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    @NotNull
    private static HttpServerConfig createConfigFromPort(final int port, final int threadCount) {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.minWorkers = threadCount;
        config.maxWorkers = threadCount;
        config.selectors = threadCount;
        return config;
    }

    private Response proxy(@NotNull final String node, @NotNull final Request request) throws IOException {
        try {
            return httpClients.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            log.error("Fail to proxy request", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Fail shutdown executor.", e);
            Thread.currentThread().interrupt();
        }

        for (final HttpClient client : httpClients.values()) {
            client.clear();
        }
    }
}
