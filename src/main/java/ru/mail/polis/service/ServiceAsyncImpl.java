package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.util.Util.toByteArray;

public class ServiceAsyncImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAsyncImpl.class);

    @NotNull
    private final DAO dao;

    @NotNull
    private final ExecutorService executor;

    ServiceAsyncImpl(
            final int port,
            @NotNull final DAO dao,
            final int workersCount,
            final int queueSize
    ) throws IOException {
        super(getConfig(port, workersCount));
        this.dao = dao;
        this.executor = new ThreadPoolExecutor(
                workersCount, workersCount,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                        .setNameFormat("async_worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Standard response for successful HTTP requests.
     *
     * @return HTTP status code 200 (OK)
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Provide access to entities.
     *
     * @param id      key of entity
     * @param request HTTP request
     * @param session HTTP session
     */
    @Path("/v0/entity")
    public void entity(
            @Param("id") final String id,
            @NotNull final Request request,
            @NotNull final HttpSession session
    ) {
        try {
            if (id == null || id.isEmpty()) {
                session.sendResponse(
                    new Response(Response.BAD_REQUEST, Response.EMPTY)
                );
                return;
            }

            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charset.defaultCharset()));
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    get(key, session);
                    break;
                }
                case Request.METHOD_PUT: {
                    put(key, request, session);
                    break;
                }
                case Request.METHOD_DELETE: {
                    delete(key, session);
                    break;
                }
                default:
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                    break;
            }
        } catch (IOException ex) {
            this.handleError(session);
        }
    }

    private void handleError(final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        } catch (IOException e) {
            logger.error("Couldn't send response", e);
        }
    }

    private void get(final ByteBuffer key, final HttpSession session) {
        this.executor.execute(() -> {
            try {
                getValue(key, session);
            } catch (IOException e) {
                logger.error("Couldn't send response", e);
            }
        });
    }

    private void getValue(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            session.sendResponse(new Response(Response.OK, toByteArray(dao.get(key))));
        } catch (NoSuchElementException ex) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException e) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void put(
        final ByteBuffer key,
        final Request request,
        final HttpSession session
    ) {
        this.executor.execute(() -> {
            try {
                dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                this.handleError(session);
            }
        });
    }

    private void delete(final ByteBuffer key, final HttpSession session) {
        this.executor.execute(() -> {
            try {
                dao.remove(key);
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                this.handleError(session);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    private static HttpServerConfig getConfig(final int port, final int workersCount) {
        final int portMin = 1024;
        final int portMax = 65536;
        if (port <= portMin || portMax <= port) {
            throw new IllegalArgumentException(
                    String.format("Invalid port value provided. It must be between %d and %d", portMin, portMax)
            );
        }

        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig server_config = new HttpServerConfig();
        server_config.maxWorkers = workersCount;
        server_config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return server_config;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown execution");
            Thread.currentThread().interrupt();
        }
    }
}
