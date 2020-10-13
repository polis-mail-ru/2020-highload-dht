package ru.mail.polis.service.manikhin;

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
import ru.mail.polis.dao.manikhin.ByteConvertor;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;


public class AsyncServiceImpl extends HttpServer implements Service {

    private final DAO dao;
    private final ExecutorService executor;
    private final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public AsyncServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        this.executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 10L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").build());
    }


    /**
     * Http status getter path.
     *
     * @return HTTP status code 200 (OK)
     */
    @Path("/v0/status")
    public void status(@NotNull HttpSession session)
    {
        Future<?> future = executor.submit(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException error) {
                log.error("Can't send response. Error: " + error);
            }
        });

        if (future.isCancelled()) {
            log.error("Error in executor");
        }
    }


    /**
     * Provide access to entities.
     *
     * @param id      key of entity
     * @param request HTTP request
     * @return response or error
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @NotNull final Request request, final HttpSession session) {

        final Future<?> future = executor.submit(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }

                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

                switch (request.getMethod()) {
                    case Request.METHOD_GET:
                        get(key, session);
                        break;
                    case Request.METHOD_PUT:
                        put(key, request, session);
                        break;
                    case Request.METHOD_DELETE:
                        delete(key, session);
                        break;
                    default:
                        session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                        break;
                }
            } catch (IOException error) {
                log.error("Response error: " + error);
            }
        });

        if (future.isCancelled()) {
            log.error("Executor error!");
        }

    }


    private void get(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] valueArray = ByteConvertor.toArray(value);
            session.sendResponse(Response.ok(valueArray));
        }  catch (IOException ex) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
        catch (NoSuchElementException error) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    private void put(final ByteBuffer key, final Request request,
                     final HttpSession session) throws IOException {
        try{
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (IOException error) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    private void delete(final ByteBuffer key, final HttpSession session) throws IOException {
        try {
            dao.remove(key);
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException error) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        final Future<?> future = executor.submit(() ->{
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException error) {
                log.error("Response error: " + error);
            }
        });

        if (future.isCancelled()) {
            log.error("Executor error!");
        }
    }

    private static HttpServerConfig getConfig(final int port) {
        if (port <= 1024 || 65536 <= port) {
            throw new IllegalArgumentException("invalid port!");
        }

        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            log.error("Can't stop server! Error: " + error);
            Thread.currentThread().interrupt();
        }
    }

}

