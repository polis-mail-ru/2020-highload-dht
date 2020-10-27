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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {

    private static final String ERROR_MESSAGE = "Can't send response. Session {}";
    private static final String RESP_ERR = "Response can't be sent: ";
    private static final String SERV_UN = "Service unavailable: ";
    private static final String BAD_REPL_PARAM = "Bad replicas-param: ";
   // private static final String MYSELF_PARAMETER = "myself";
   // private static final String REMOVED_PERMANENTLY = "310 Removed Permanently";
    //private static final String DELETED_SPECIAL_VALUE = "P**J$#RFh7e3j89ri(((8873uj33*&&*&&";


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
        final Map<String, HttpClient> clientsQueue = new HashMap<>();
        for (final String it : simpleTopology.getNodes()) {
            if (!simpleTopology.getMe().equals(it) && !clientsQueue.containsKey(it)) {
                clientsQueue.put(it, new HttpClient(new ConnectionString(it + "?timeout=100")));
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

    private Future<Response> forwardRequestFuture (@NotNull final String cluster,
                                    final Request request) throws IOException {
        try {
            return service.submit(() -> forwardRequest(cluster, request));
        } catch (RejectedExecutionException ex) {
            log.error(SERV_UN, ex);
            return null;
        }
    }

    private Response forwardRequest(@NotNull final String cluster,
                                      final Request request) throws IOException {
        try {
            return clients.get(cluster).invoke(request);
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
                       final @Param(value = "replicas") String replicasParam,
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
        final Replica replicas;
        try {
            replicas = new Replica(replicasParam, simpleTopology.getNodes().length);
        } catch (ReplicasParamParseException ex) {
            log.error(BAD_REPL_PARAM, ex);
            trySendResponse(session, new Response(Response.BAD_REQUEST));
            return;
        }
        sendToReplicas(id, replicas, session, request);
    }

    private void sendToReplicas(final @Param(value = "id", required = true) String key,
                                @NotNull final Replica replicas,
                                @NotNull final HttpSession session,
                                final @Param("request") Request request) throws IOException {
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
            final ByteBuffer key = strToByteBuffer(id, UTF_8);
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
                dao.remove(id);
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
            log.error(RESP_ERR, session, ex);
        }
    }

    /*private static Request addMyselfParamToRequest(final Request request) {
        if (request.getParameter(MYSELF_PARAMETER) != null) {
            return request;
        }
        final var newURI = request.getURI() + "&" + MYSELF_PARAMETER + "=";
        final var res = new Request(request.getMethod(), newURI, request.isHttp11());
        for (int i = 0; i < request.getHeaderCount(); i++) {
            res.addHeader(request.getHeaders()[i]);
        }
        res.setBody(request.getBody());
        return res;
    }*/

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    public static ByteBuffer strToByteBuffer(final String msg, final Charset charset) {
        return ByteBuffer.wrap(msg.getBytes(charset));
    }
}
