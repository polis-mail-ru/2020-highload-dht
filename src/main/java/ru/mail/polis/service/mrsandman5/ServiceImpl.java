package ru.mail.polis.service.mrsandman5;

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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final Response ERROR = new Response(Response.BAD_REQUEST, Response.EMPTY);
    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executorService;

    public ServiceImpl(final int port,
                       @NotNull final DAO dao,
                       final int pools) throws IOException {
        super(config(port));
        this.dao = dao;
        this.executorService = new ThreadPoolExecutor(pools, pools,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(32));
    }

    /** Request method for HTTP server.
     * @param id - id request.
     * @param request - type of request.
     * */
    @Path("/v0/entity")
    public void response(@Param(value = "id", required = true) final String id,
                             final Request request,
                             @NotNull final HttpSession session) {
        log.debug("Request handling : {}", id);
        if (id.isEmpty()) {
            try {
                session.sendResponse(ERROR);
            } catch (IOException e) {
                log.error("Can't send response", e);
            }
        }

        final var key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                get(key, session);
                break;
            case Request.METHOD_PUT:
                put(key, request.getBody(), session);
                break;
            case Request.METHOD_DELETE:
                delete(key, session);
                break;
            default:
                log.warn("Non-supported request : {}", id);
                try {
                    session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Can't send response", e);
                }
                break;
        }
    }

    private void get(@NotNull final ByteBuffer key,
                     @NotNull final HttpSession session) {
        final ByteBuffer[] value = new ByteBuffer[1];
        executorService.execute(() -> {
            try {
                try {
                    value[0] = dao.get(key);
                } catch (NoSuchElementException e) {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException e) {
                    log.error("GET error : {}", toByteArray(key));
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }
                session.sendResponse(Response.ok(toByteArray(value[0])));
            } catch (IOException ex) {
                log.error("Can't send response", ex);
            }
        });
    }

    private void put(@NotNull final ByteBuffer key,
                          final byte[] body,
                     @NotNull final HttpSession session) {
        final ByteBuffer value = ByteBuffer.wrap(body);
        executorService.execute(() -> {
            try {
                try {
                    dao.upsert(key, value);
                } catch (IOException e) {
                    log.error("PUT error : {} with value {}", toByteArray(key), toByteArray(value));
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException ex) {
                log.error("Can't send response", ex);
            }
        });
    }

    private void delete(@NotNull final ByteBuffer key,
                        @NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                try {
                    dao.remove(key);
                } catch (IOException e) {
                    log.error("DELETE error : {}", toByteArray(key));
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                }

                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException ex) {
                log.error("Can't send response", ex);
            }
        });
    }

    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                log.error("Can't send response", e);
            }
        });
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        log.error("Invalid request : {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @NotNull
    private static HttpServerConfig config(final int port) {
        final var acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final var config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }

        final var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Can't shutdown executor", e);
        }
    }
}
