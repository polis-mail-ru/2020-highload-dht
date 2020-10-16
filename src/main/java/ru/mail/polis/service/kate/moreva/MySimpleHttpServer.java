package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple Http Server Service implementation.
 *
 * @author Kate Moreva
 */

public class MySimpleHttpServer extends HttpServer implements Service {

    private static final String SERVER_ERROR = "Server error can't send response {}";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);
    private final DAO dao;
    private final ExecutorService executorService;

    /**
     * Http Server constructor.
     */
    public MySimpleHttpServer(final int port,
                              final DAO dao,
                              final int numberOfWorkers,
                              final int queueSize) throws IOException {
        super(getConfig(port, numberOfWorkers));
        this.dao = dao;
        assert numberOfWorkers > 0;
        assert queueSize > 0;
        this.executorService = new ThreadPoolExecutor(numberOfWorkers,
                queueSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in {} when processing request", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private static HttpServerConfig getConfig(final int port, final int numberOfWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numberOfWorkers;
        config.maxWorkers = numberOfWorkers;
        config.minWorkers = numberOfWorkers;
        return config;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
    }

    /**
     * Method to check whether the server is reachable or not.
     * If the server is available @return {@link Response} {@code 200}.
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        executorService.execute(() -> {
            try {
                session.sendResponse(new Response(Response.OK, Response.EMPTY));
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
        });
    }

    /**
     * Method for working with value in the DAO by the key.
     *
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 201} (new data created).
     * {@code 202} (data deleted).
     * {@code 405} (unexpected method).
     * {@code 500} (internal server error occurred).
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       final Request request,
                       final HttpSession session) {
        if (id.isBlank()) {
            log.error("Request with empty id on /v0/entity");
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                getMethod(key, session);
                break;
            case Request.METHOD_PUT:
                putMethod(key, request, session);
                break;
            case Request.METHOD_DELETE:
                deleteMethod(key, session);
                break;
            default:
                log.error("Not allowed method on /v0/entity");
                try {
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                } catch (IOException e) {
                    log.error(SERVER_ERROR, session, e);
                }
                break;
        }
    }

    private void getMethod(final ByteBuffer key, final HttpSession session) {
        executorService.execute(() -> {
            try {
                getEntity(key, session);
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
        });
    }

    private void putMethod(final ByteBuffer key, final Request request, final HttpSession session) {
        executorService.execute(() -> {
            try {
                putEntity(key, request, session);
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
        });
    }

    private void deleteMethod(final ByteBuffer key, final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteEntity(key, session);
            } catch (IOException e) {
                log.error(SERVER_ERROR, session, e);
            }
        });
    }

    /**
     * Subsidiary method to get value.
     *
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    private void getEntity(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] body = new byte[value.remaining()];
            value.get(body);
            session.sendResponse(new Response(Response.OK, body));
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        } catch (IOException e) {
            log.error("GET method failed on /v0/entity for id {}", key.get(), e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Subsidiary method to put new value.
     *
     * {@code 201} (new data created).
     * {@code 500} (internal server error occurred).
     */
    private void putEntity(final ByteBuffer key, final Request request, final HttpSession session) throws IOException {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException e) {
            log.error("PUT method failed on /v0/entity for id {}, request body {}.", key.get(), request.getBody(), e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Subsidiary method to delete value by the key.
     *
     * {@code 202} (data deleted).
     * {@code 500} (internal server error occurred).
     */
    private void deleteEntity(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            dao.remove(key);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException e) {
            log.error("DELETE method failed on /v0/entity for id {}.", key.get(), e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error can't shutdown execution service");
            Thread.currentThread().interrupt();
        }
    }
}
