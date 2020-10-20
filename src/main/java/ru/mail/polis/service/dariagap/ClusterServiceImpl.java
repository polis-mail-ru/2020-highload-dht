package ru.mail.polis.service.dariagap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ClusterServiceImpl extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    private final ExecutorService exec;
    private final Set<String> topology;
    private final HashMap<String,HttpClient> clients;
    private final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final String EXECUTOR_ERROR = "Error in executor";
    private static final String RESPONSE_ERROR = "Can not send response.";
    private static final String PROXY_ERROR = "Can not proxy request";


    /**
     * Config HttpServer, DAO and ExecutorService.
     *
     * @param port - to accept HTTP server
     * @param dao - storage interface
     * @param executors - number of executors
     * @param queueSize - size of queue in ThreadPoolExecutor
     * @param topology - set of cluster nodes
     */
    public ClusterServiceImpl(
            final int port,
            @NotNull final DAO dao,
            @NotNull final int executors,
            @NotNull final int queueSize,
            @NotNull final Set<String> topology) throws IOException {
        super(formConfig(port));
        HttpClient client;
        this.dao = dao;
        this.exec = new ThreadPoolExecutor(
                executors,
                executors,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Executor-%d")
                        .build()
                );
        this.clients = new HashMap<>();
        this.topology = topology;
        for (final String s: topology) {
            if (!isCurrentNode(s)) {
                client = new HttpClient(new ConnectionString(s));
                client.setTimeout(100);
                this.clients.put(s,client);
            }
        }
    }

    private Boolean isCurrentNode(final String node) {
        return node.equals("http://localhost:" + this.port);
    }

    private static HttpServerConfig formConfig(final int port) {
        final HttpServerConfig conf = new HttpServerConfig();
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        conf.acceptors = new AcceptorConfig[]{ac};
        return conf;
    }

    /**
     * Status OK answer.
     *
     * @param session - HttpSession
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        final Future<?> future = exec.submit(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException ex) {
                log.error(RESPONSE_ERROR, ex);
            }
        });

        if (future.isCancelled()) {
            log.error(EXECUTOR_ERROR);
        }
    }

    /**
     * Get, set or delete data by id.
     *
     * @param id key of entity
     * @param request request with the entity value in body (for METHOD_PUT)
     * @param session - HttpSession
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param("request") final Request request,
                       final HttpSession session) {
        final Future<?> future = exec.submit(() -> {
            final String requestNode;
            final Response response;
            try {
                if (id.isEmpty()) {
                    response = new Response(Response.BAD_REQUEST, Response.EMPTY);
                } else {
                    requestNode = Util.getNode(topology,id);
                    if (!isCurrentNode(requestNode)) {
                        response = proxy(clients.get(requestNode),request);
                    } else {
                        switch (request.getMethod()) {
                            case METHOD_GET:
                                response = getSync(id);
                                break;
                            case METHOD_PUT:
                                response = putSync(id, request);
                                break;
                            case METHOD_DELETE:
                                response = deleteSync(id);
                                break;
                            default:
                                log.error("Unknown method");
                                response =
                                        new Response(Response.METHOD_NOT_ALLOWED,
                                                Response.EMPTY);
                                break;
                        }
                    }
                }
                session.sendResponse(response);
            } catch (Exception ex) {
                log.error(RESPONSE_ERROR, ex);
            }
        });

        if (future.isCancelled()) {
            log.error(EXECUTOR_ERROR);
        }
    }

    private Response proxy(final HttpClient client, final Request request) {
        try {
            return client.invoke(request);
        } catch (Exception e) {
            log.error(PROXY_ERROR, e);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private Response getSync(final String id) throws IOException {
        try {
            final ByteBuffer value = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.OK, Util.byteBufferToBytes(value));
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private Response putSync(final String id,
                         final Request request) throws IOException {
        try {
            dao.upsert(ByteBuffer.wrap(id.getBytes(UTF_8)),
                    ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response deleteSync(final String id) throws IOException {
        try {
            dao.remove(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Future<?> future = exec.submit(() -> {
            try {
                final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
                session.sendResponse(response);
            } catch (IOException ex) {
                log.error(RESPONSE_ERROR, ex);
            }
        });

        if (future.isCancelled()) {
            log.error(EXECUTOR_ERROR);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Can not stop server.", ex);
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clients.values()) {
            client.clear();
        }
    }
}
