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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {

    private static final String ERROR_MESSAGE = "Can't send response. Session {}";

    @NotNull private final DAO dao;
    private final ExecutorService service;
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final SimpleTopology simpleTopology;
    @NotNull private final Map<String, HttpClient> clients;

    /**
     * Constructor.
     *  @param port - service configuration.
     * @param dao - dao implementation.
     * @param simpleTopology - topology
     */
    public AsyncServiceImpl(final int port, @NotNull final DAO dao,
                            @NotNull final SimpleTopology simpleTopology) throws IOException {
        super(createConfig(port));
        this.dao = dao;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").build());
        this.simpleTopology = simpleTopology;
        final Map<String, HttpClient> clientsQueue = new HashMap<>();
        for (final String it : simpleTopology.getNodes()) {
            if (!simpleTopology.getMe().equals(it) && !clientsQueue.containsKey(it)) {
                final var client = new HttpClient(new ConnectionString(it + "?timeout=1000"));
                client.setConnectTimeout(100);
                client.setTimeout(100);
                client.setReadTimeout(100);
                clientsQueue.put(it, client);
            }
        }
        this.clients = clientsQueue;
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

    private Future<Response> forwardRequestFuture(@NotNull final String cluster, final Request request) {
        try {
            return service.submit(() -> forwardRequest(cluster, request));
        } catch (RejectedExecutionException ex) {
            log.error("Service unavailable: ", ex);
            return null;
        }
    }

    private Response forwardRequest(@NotNull final String cluster,
                                      final Request request) throws HttpException, PoolException, InterruptedException {

        try {
            return clients.get(cluster).invoke(SimpleTopology.getSpecialRequest(request));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
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
    public void entity(@NotNull @Param(value = "id", required = true) final String id, @NotNull final Request request,
                       final @Param(value = "special") String special,
                       final @Param(value = "replicas") String replicasParam,
                       final HttpSession session) {
        if (id.isEmpty()) {
            trySendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        if (special != null) {
            final var resp = processRequest(id, request);
            try {
                if (resp == null) {
                    trySendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } else {
                    trySendResponse(session, resp.get());
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Service unavailable: ", e);
            }
            return;
        }
        final Replica replicas;
        try {
            replicas = new Replica(replicasParam, simpleTopology.getNodes().length);
        } catch (ReplicasParamParseException ex) {
            log.error("Bad replicas-param: ", ex);
            trySendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        sendToReplicas(id, replicas, session, request);
    }

    private void sendToReplicas(final @Param(value = "id", required = true) String key,
                                @NotNull final Replica replicas, @NotNull final HttpSession session,
                                final @Param("request") Request request) {
        final var responsibleNodes = simpleTopology.responsibleNodes(key, replicas);
        final var answers = new ArrayList<Future<Response>>(responsibleNodes.size());
        for (final var node : responsibleNodes) {
            if (simpleTopology.getMe().equals(node)) {
                answers.add(processRequest(key, request));
            } else {
                answers.add(forwardRequestFuture(node, request));
            }
        }
        final var composer = new ResponseComposer();
        for (final var answer : answers) {
            if (answer == null) {
                continue;
            }
            final Response response;
            try {
                response = answer.get();
            } catch (InterruptedException | ExecutionException e) {
                continue;
            }
            composer.addResponse(response, replicas.getAckCount());
        }
        final var requiredResponse = composer.getComposedResponse();
        trySendResponse(session, requiredResponse);
    }

    private Future<Response> processRequest(final @Param(value = "id", required = true) String id,
                                            final @Param("request") Request request) {
        try {
            final ByteBuffer key = Converter.strToByteBuffer(id, UTF_8);
            return service.submit(() -> {
                switch (request.getMethod()) {
                    case METHOD_GET:
                        return get(key);
                    case METHOD_PUT:
                        return put(key, request);
                    case METHOD_DELETE:
                        return delete(key);
                    default:
                        return null;
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("Error in ServiceImpl.get() method; internal error: ", ex);
            return null;
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

    /**
     * Get data by key.
     *
     * @param id      - key.
     */
    public Response get(@NotNull @Param(value = "id", required = true) final ByteBuffer id) {
        try {
            final ByteBuffer val;
            val = dao.get(id);
            return Response.ok(Converter.fromByteBufferToByteArray(val));
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("Error in get request", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Put data by key.
     *
     * @param id      - key.
     * @param request - request.
     */
    public Response put(@NotNull @Param(value = "id", required = true) final ByteBuffer id,
                    @NotNull @Param(value = "request", required = true) final Request request) {
        try {
            final ByteBuffer value = ByteBuffer.wrap(request.getBody());
            dao.upsert(id, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ex) {
            log.error(ERROR_MESSAGE, ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete data by key.
     *
     * @param id      - key.
     */
    public Response delete(@NotNull @Param(value = "id", required = true) final ByteBuffer id) {
            try {
                dao.upsert(id, ByteBuffer.wrap("deleted".getBytes(UTF_8)));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (NoSuchElementException e) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } catch (IOException e) {
                log.error("Error in delete request", e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
    }

    private void trySendResponse(final HttpSession session,
                                 final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException ex) {
            log.error("Response can't be sent:", session, ex);
        }
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        service.shutdown();
        try {
            if (!service.awaitTermination(1, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
        for (final var client : clients.entrySet()) {
            client.getValue().close();
        }
        clients.clear();
    }
}
