package ru.mail.polis.service.mariarheon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    @NotNull
    private final DAO dao;
    private final ExecutorService service;
    private final RendezvousSharding sharding;
    private static final String RESP_ERR = "Response can't be sent: ";
    private static final String SERV_UN = "Service unavailable: ";
    private static final String BAD_REPL_PARAM = "Bad replicas-param: ";
    private static final String MYSELF_PARAMETER = "myself";
    private static final String REMOVED_PERMANENTLY = "310 Removed Permanently";
    private static final String DELETED_SPECIAL_VALUE = "P**J$#RFh7e3j89ri(((8873uj33*&&*&&";

    /**
     * Asynchronous Service Implementation.
     *
     * @param config - configuration.
     * @param dao - dao
     */
    public AsyncServiceImpl(final HttpServerConfig config,
                            @NotNull final DAO dao,
                            @NotNull final RendezvousSharding sharding) throws IOException {
        super(config);
        this.dao = dao;
        this.sharding = sharding;
        final int workers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(workers, workers, 0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                    .setNameFormat("async_workers-%d")
                .build()
                );
    }

    /** Get/set/delete key-value entity.
     *
     * @param key - record id.
     * @param replicasParam - ack/from, where ack is how many answers should be retrieved,
     *                 from - total count of nodes
     * @param session - session.
     * @param request - request.
     **/
    @Path("/v0/entity")
    @RequestMethod({METHOD_GET, METHOD_PUT, METHOD_DELETE})
    public void handleEntityRequest(final @Param(value = "id", required = true) String key,
                    final @Param(value = "replicas") String replicasParam,
                    final @Param(value = MYSELF_PARAMETER) String myself,
                    @NotNull final HttpSession session,
                    final @Param("request") Request request) {
        if (key.isEmpty()) {
            trySendResponse(session, new ZeroResponse(Response.BAD_REQUEST));
            return;
        }
        if (myself != null) {
            final var resp = processRequest(key, request);
            try {
                if (resp == null) {
                    trySendResponse(session, new ZeroResponse(Response.INTERNAL_ERROR));
                } else {
                    trySendResponse(session, resp.get());
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(SERV_UN, e);
            }
            return;
        }
        final Replicas replicas;
        try {
            replicas = new Replicas(replicasParam, sharding.getNodesCount());
        } catch (ReplicasParamParseException ex) {
            logger.error(BAD_REPL_PARAM, ex);
            trySendResponse(session, new ZeroResponse(Response.BAD_REQUEST));
            return;
        }
        sendToReplicas(key, replicas, session, request);
    }

    private void sendToReplicas(final @Param(value = "id", required = true) String key,
                                @NotNull final Replicas replicas,
                                @NotNull final HttpSession session,
                                final @Param("request") Request request) {
        final var responsibleNodes = sharding.getResponsibleNodes(key, replicas);
        final var answers = new ArrayList<Future<Response>>(responsibleNodes.size());
        for (final var node : responsibleNodes) {
            if (sharding.isMe(node)) {
                answers.add(processRequest(key, request));
            } else {
                answers.add(passOn(node, request));
            }
        }
        final var composer = new ReplicasResponseComposer();
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

    private Future<Response> processRequest(final @Param(value = "id", required = true) String key,
                                final @Param("request") Request request) {
        try {
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
            logger.error(SERV_UN, ex);
            return null;
        }
    }

    private Response get(final String key) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.getInternal() method: key is empty");
                return new ZeroResponse(Response.BAD_REQUEST);
            }
            final ByteBuffer response = dao.get(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)));
            final var ar = ByteBufferUtils.toArray(response);
            if (Arrays.equals(ar, DELETED_SPECIAL_VALUE.getBytes(StandardCharsets.UTF_8))) {
                return new ZeroResponse(REMOVED_PERMANENTLY);
            }
            return Response.ok(ar);
        } catch (NoSuchElementException ex) {
            return new ZeroResponse(Response.NOT_FOUND);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.getInternal() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
    }

    private Response put(final String key,
                     final Request request) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.putInternal() method: key is empty");
                return new ZeroResponse(Response.BAD_REQUEST);
            }
            dao.upsert(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)),
                    ByteBufferUtils.toByteBuffer(request.getBody()));
            return new ZeroResponse(Response.CREATED);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.putInternal() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
    }

    private Response delete(final String key) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.delete() method: key is empty");
                return new ZeroResponse(Response.BAD_REQUEST);
            }
            dao.upsert(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)),
                    ByteBufferUtils.toByteBuffer(DELETED_SPECIAL_VALUE.getBytes(StandardCharsets.UTF_8)));
            return new ZeroResponse(Response.ACCEPTED);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.delete() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
    }

    private Future<Response> passOn(@NotNull final String reqNode,
                                    @NotNull final Request request) {
        try {
            return service.submit(() -> passOnInternal(reqNode, request));
        } catch (RejectedExecutionException ex) {
            logger.error(SERV_UN, ex);
            return null;
        }
    }

    private Response passOnInternal(@NotNull final String reqNode,
                                @NotNull final Request request) {
        try {
            return sharding.passOn(reqNode, addMyselfParamToRequest(request));
        } catch (InterruptedException | IOException | HttpException | PoolException e) {
            logger.error("Failed to pass on the request: ", e);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
    }

    private void trySendResponse(final HttpSession session,
                                 final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException ex) {
            logger.error(RESP_ERR, session, ex);
        }
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        trySendResponse(session, new ZeroResponse(Response.OK));
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        trySendResponse(session, new ZeroResponse(Response.BAD_REQUEST));
    }

    private static Request addMyselfParamToRequest(final Request request) {
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
    }
}
