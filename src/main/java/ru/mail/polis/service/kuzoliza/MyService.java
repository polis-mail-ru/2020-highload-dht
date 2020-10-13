package ru.mail.polis.service.kuzoliza;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyService extends HttpServer {

    private final DAO dao;
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    private final ExecutorService executor;

    MyService(final HttpServerConfig config, final DAO dao, final int workers, final int queue) throws IOException {
        super(config);
        this.dao = dao;
        assert workers > 0;
        assert queue > 0;
        executor = new ThreadPoolExecutor(workers, queue, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queue),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error processing request {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public void status(final HttpSession session) {
        try {
            executor.execute(() -> {
                try {
                    session.sendResponse(Response.ok("OK"));
                } catch (IOException e) {
                    log.error("Can't send OK response", e);
                }
            });
        } catch (Exception e) {
            log.error("Execution error", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ex) {
                log.error("Can't send 500 response", ex);
            }
        }
    }

    /**
     * Put/delete and get data from dao.
     *
     * @param id - key
     * @param request - body of request
     *
     * @return - code 200 - "success" - successfully got data
     *           code 201 - "created" - successful respond to put request
     *           code 202 - "accepted" - successfully deleted data
     *           code 400 - "bad request" - syntax error (id is empty)
     *           code 404 - "not found" - server can't find the resource
     *           code 405 - "method is not allowed" - method can't be used
     *           code 500 - "internal error" - server is not responding
     */
    @Path("/v0/entity")
    public void entity(final @Param(value = "id", required = true) String id, final Request request, final HttpSession session) {
        try {
            executor.execute(() -> {
                if (id.isEmpty()) {
                    try {
                        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    } catch (IOException e) {
                        log.error("Can't send bad response", e);
                    }
                }

                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
                response(key, request, session);
            });

        } catch (Exception e) {
            log.error("Execution error", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ex) {
                log.error("Can't send 500 response", ex);
            }
        }

    }

    private void response(final ByteBuffer key, final Request request, final HttpSession session) {
        try {
            executor.execute(() -> {
                switch (request.getMethod()) {
                    case Request.METHOD_GET:
                        try {
                            final ByteBuffer value = dao.get(key);
                            try {
                                session.sendResponse(Response.ok(toByteArray(value)));
                            } catch (IOException e) {
                                log.error("Can't send OK response", e);
                            }
                        } catch (NoSuchElementException e) {
                            log.debug("Can't find resource {}", key, e);
                            try {
                                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                            } catch (IOException ex) {
                                log.error("Can't send 404 response", e);
                            }
                        } catch (IOException e) {
                            log.error("Can't send OK response", e);
                        }
                        break;

                    case Request.METHOD_PUT:
                        try {
                            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                        } catch (IOException e) {
                            log.error("Can't upsert element", e);
                        }
                        try {
                            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                        } catch (IOException e) {
                            log.error("Can't send 201 response", e);
                        }
                        break;

                    case Request.METHOD_DELETE:
                        try {
                            dao.remove(key);
                        } catch (IOException e) {
                            log.error("Can't remove element", e);
                        }
                        try {
                            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                        } catch (IOException e) {
                            log.error("Can't send 202 response", e);
                        }
                        break;

                    default:
                        try {
                            session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
                        } catch (IOException e) {
                            log.error("Can't send 405 response", e);
                        }
                        break;
                }
            });
        } catch (Exception e) {
            log.error("Execution error", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ex) {
                log.error("Can't send 500 response", ex);
            }
        }
    }

    private static byte[] toByteArray(final ByteBuffer value) {
        if (!value.hasRemaining()) {
            return Response.EMPTY;
        }

        final byte[] response = new byte[value.remaining()];
        value.get(response);
        return response;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        try {
            executor.execute(() -> {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Can't send bad response", e);
                }
            });

        } catch (Exception e) {
            log.error("Execution error", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ex) {
                log.error("Can't send 500 response", ex);
            }
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Can't shutdown executor", e);
        }
    }
}
