package ru.mail.polis.service.ivanovandrey;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {

    private static final String ERROR_MESSAGE = "Can't send response. Session {}";
    private static final String REJ_ERR = "Rejected  exception: ";
    private static final String OVF_ERR = "Queue overflowed: ";
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);

    @NotNull
    private final DAO dao;
    private final SimpleTopology simpleTopology;
    @NotNull
    private final Map<String, HttpClient> clients;
    private final ExecutorService executor;
    
    /**
     * Constructor.
     *  @param port - service configuration.
     * @param dao - dao implementation.
     * @param simpleTopology - topology
     */
    public AsyncServiceImpl(final int port,
                            @NotNull final DAO dao,
                            @NotNull final int queueSize,
                            @NotNull final int countOfWorkers,
                            @NotNull final SimpleTopology simpleTopology) throws IOException {
        super(createConfig(port));
        this.dao = dao;
        executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in async_worker-{} : ", t, e))
                        .setNameFormat("async_worker-%d")
                        .build());
        this.simpleTopology = simpleTopology;
        final Map<String, HttpClient> clientsMap = new HashMap<>();
        for (final String it : simpleTopology.getNodes()) {
            if (!simpleTopology.getMe().equals(it) && !clientsMap.containsKey(it)) {
                clientsMap.put(it, new HttpClient(new ConnectionString(it + "?timeout=1000")));
            }
        }
        this.clients = clientsMap;
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{ac};
        return httpServerConfig;
    }

    private Response forwardRequest(@NotNull final String node,
                                             final Request request) throws IOException {
        try {
            return clients.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            throw new IOException("fail", e);
        }
    }

    /**
     * Get, Delete or Put data by key.
     *
     * @param id      - key.
     * @param session - session.
     * @param request - request.
     */
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
        final String keyNode = simpleTopology.primaryFor(key);

        if (!simpleTopology.getMe().equals(keyNode)) {
            executor.execute(() -> {
                try {
                    final var resp = forwardRequest(keyNode, request);
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
                        asyncGet(key, session);
                        break;
                    case METHOD_DELETE:
                        asyncDelete(key, session);
                        break;
                    case METHOD_PUT:
                        asyncPut(key, request, session);
                        break;
                    default:
                        session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
                        break;
                }
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
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                log.error(ERROR_MESSAGE, session, e);
            }
    }

    /**
     * Get data by key.
     *
     * @param id      - key.
     * @param session - session.
     */
    public void asyncGet(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                         final HttpSession session) {
        executor.execute(() -> {
        try {
            get(id, session);
        } catch (IOException e) {
                log.error("Error in get request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
        } catch (RejectedExecutionException e) {
            log.error(REJ_ERR, e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (IOException e1) {
                log.error(OVF_ERR, e1);
            }
        } catch (NoSuchElementException e) {
            try {
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException i) {
                log.error(ERROR_MESSAGE, session, i);
            }
        }
        });
    }

    /** Get data by key method.
     * @param id - id request.
     * @param session - session.
     **/
    public void get(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    final HttpSession session) throws IOException {
            final ByteBuffer val = dao.get(id);
            session.sendResponse(Response.ok(Converter.fromByteBufferToByteArray(val)));
    }

    /** Async put/update data by key method.
     * @param id - id request.
     * @param request - data.
     * @param session - session.
     **/
    public void asyncPut(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    @NotNull @Param(value = "request", required = true) final Request request,
                    @NotNull final HttpSession session) {
        executor.execute(() -> {
            try {
                put(id,request,session);
            } catch (IOException e) {
                log.error("Error in delete request", e);

                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            } catch (RejectedExecutionException e) {
                log.error(REJ_ERR, e);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException e1) {
                    log.error(OVF_ERR, e1);
                }
            }
        });
    }

    /**
     * Put data by key.
     *
     * @param id      - key.
     * @param session - session.
     * @param request - request.
     */
    public void put(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    @NotNull @Param(value = "request", required = true) final Request request,
                    @NotNull final HttpSession session) throws IOException {
                final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                dao.upsert(id, value);
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    /** Async delete data by key method.
     * @param id - key request.
     * @param session - session.
     **/
    public void asyncDelete(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                       @NotNull final HttpSession session) {
        executor.execute(() -> {
            try {
                delete(id,session);
            } catch (IOException e) {
                log.error("Error in delete request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            } catch (RejectedExecutionException e) {
                log.error(REJ_ERR, e);
                try {
                    session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
                } catch (IOException e1) {
                    log.error(OVF_ERR, e1);
                }
            }
        });
    }

    /**
     * Delete data by key.
     *
     * @param id      - key.
     * @param session - session.
     **/
    public void delete(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                       @NotNull final HttpSession session) throws IOException {
                dao.remove(id);
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
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
