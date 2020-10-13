package ru.mail.polis.service.gogun;

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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AsyncServiceImpl extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    private final ExecutorService executorService;

    /**
     * class that provides requests to lsm dao via http.
     *
     * @param port - port
     * @param numWorkers - num of threads in executor service
     * @param queueSize - thread queue size
     * @param dao - key-value storage
     * @throws IOException - ioexception
     */
    public AsyncServiceImpl(final int port,
                            final int numWorkers,
                            final int queueSize,
                            final DAO dao) throws IOException {
        super(makeConfig(port, numWorkers));
        this.dao = dao;
        executorService = new ThreadPoolExecutor(numWorkers,
                queueSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error {} in {}", e, t))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @NotNull
    private static HttpServerConfig makeConfig(final int port, final int numWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numWorkers;
        config.maxWorkers = numWorkers;
        config.minWorkers = numWorkers;
        return config;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        executorService.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                log.error("Can't understand request {}", request);
            }
        });
    }


    /**
     * provide checking api is alive.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        executorService.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    private ByteBuffer getBuffer(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    private byte[] getArray(final ByteBuffer buffer) {
        byte[] body;
        if (buffer.hasRemaining()) {
            body = new byte[buffer.remaining()];
            buffer.get(body);
        } else {
            body = Response.EMPTY;
        }

        return body;
    }

    private void handleGetRequest(final String id, final HttpSession session) throws IOException {
        log.debug("GET request with id: {}", id);

        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }

        ByteBuffer buffer = ByteBuffer.allocate(0);

        try {
            buffer = dao.get(getBuffer(id.getBytes(UTF_8)));
        } catch (IOException e) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }

        session.sendResponse(Response.ok(getArray(buffer)));
    }

    /**
     * Provide getting data by id from lsm.
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) @NotNull final String id, final HttpSession session) {
        executorService.execute(() -> {
            try {
                handleGetRequest(id, session);
            } catch (IOException e) {
                log.error("Error sending response in method get {}", e.getMessage());
            }
        });
    }

    private void handlePutRequest(final String id,
                                  final Request request,
                                  final HttpSession session) throws IOException {
        log.debug("PUT request with id: {}", id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }

        try {
            dao.upsert(getBuffer(id.getBytes(UTF_8)), getBuffer(request.getBody()));
        } catch (IOException e) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }

        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    /**
     * Provide putting/updating data by key.
     *
     * @param id      - key
     * @param request - value
     * @param session - session
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void upsert(@Param(value = "id", required = true) @NotNull final String id,
                       final Request request,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                handlePutRequest(id, request, session);
            } catch (IOException e) {
                log.error("Error sending response in method put {}", e.getMessage());
            }
        });
    }

    private void handleDelRequest(final String id, final HttpSession session) throws IOException {
        log.debug("DELETE request with id: {}", id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }

        try {
            dao.remove(getBuffer(id.getBytes(UTF_8)));
        } catch (IOException e) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }

        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
    }

    /**
     * Provide deleting by id.
     *
     * @param id      - key
     * @param session - session
     *
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) @NotNull final String id, final HttpSession session) {
        executorService.execute(() -> {
            try {
                handleDelRequest(id, session);
            } catch (IOException e) {
                log.error("Error sending response in method del {}", e.getMessage());
            }
        });
    }
}
