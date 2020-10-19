package ru.mail.polis.service.ivanovandrey;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.Converter;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {

    private static final String ERROR_MESSAGE = "Can't send response. Session {}";

    @NotNull
    private final DAO dao;
    private final ExecutorService service;
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final SimpleTopology simpleTopology;
    @NotNull
    private final Map<String, HttpClient> clients;

    /**
     * Constructor.
     *  @param port - service configuration.
     * @param dao - dao implementation.
     * @param simpleTopology - topology
     */
    public AsyncServiceImpl(final int port,
                            @NotNull final DAO dao,
                            @NotNull final SimpleTopology simpleTopology) throws IOException {
        super(createConfig(port));
        this.dao = dao;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                        .setNameFormat("async_worker-%d")
                        .build());
        this.simpleTopology = simpleTopology;
        final Map<String, HttpClient> clients = new HashMap<>();
        for (final String it : simpleTopology.getNodes()) {
            if (!simpleTopology.getMe().equals(it) && !clients.containsKey(it)) {
                clients.put(it, new HttpClient(new ConnectionString(it + "?timeout=100")));
            }
        }
        this.clients = clients;
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.maxWorkers = Runtime.getRuntime().availableProcessors();
        httpServerConfig.queueTime = 10;
        httpServerConfig.acceptors = new AcceptorConfig[]{ac};
        return httpServerConfig;
    }

    private Response forwardRequest(@NotNull final String cluster,
                                      final Request request) throws IOException {
        try {
            return clients.get(cluster).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            throw new IOException("fail", e);
        }
    }

    @Path("/v0/entity")
    @RequestMethod({METHOD_GET, METHOD_PUT, METHOD_DELETE})
    public void entity(@NotNull @Param(value = "id", required = true) final String id,
                       @NotNull final Request request,
                       final HttpSession session) throws IOException {
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        final String keyCluster = simpleTopology.primaryFor(key);

        if (!simpleTopology.getMe().equals(keyCluster)) {
            service.execute(() -> {
                try {
                    final var resp = forwardRequest(keyCluster, request);
                    session.sendResponse(resp);
                } catch (IOException e) {
                    try {
                        session.sendError(Response.INTERNAL_ERROR, e.getMessage());
                    } catch (IOException ex) {
                        log.error(ERROR_MESSAGE, session, ex);
                    }
                }
            });
            return;
        }

        try {
                switch (request.getMethod()) {
                    case METHOD_GET:
                        get(key, session);
                        break;
                    case METHOD_DELETE:
                        delete(key, session);
                        break;
                    case METHOD_PUT:
                        put(key, request, session);
                        break;
                    default:
                        session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
                        break;
            };
        } catch (IOException ex) {
            log.error("Error in ServiceImpl.get() method; internal error: ", ex);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                log.error(ERROR_MESSAGE, session, e);
            }
        });
    }

    public void get(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    final HttpSession session) {
        service.execute(() -> {
            try {
                final ByteBuffer val;
                val = dao.get(id);
                session.sendResponse(Response.ok(Converter.fromByteBufferToByteArray(val)));
            } catch (NoSuchElementException e) {
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException i) {
                    log.error(ERROR_MESSAGE, session, e);
                }
            } catch (IOException e) {
                log.error("Error in get request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    public void put(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    @NotNull @Param(value = "request", required = true) final Request request,
                    @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                dao.upsert(id, value);
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                log.error("Error in delete request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    public void delete(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                       @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                dao.remove(id);
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (NoSuchElementException e) {
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException i) {
                    log.error(ERROR_MESSAGE, session, e);
                }
            } catch (IOException e) {
                log.error("Error in delete request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    public static ByteBuffer strToByteBuffer(final String msg, final Charset charset) {
        return ByteBuffer.wrap(msg.getBytes(charset));
    }
}
