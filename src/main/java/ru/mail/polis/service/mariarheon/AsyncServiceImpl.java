package ru.mail.polis.service.mariarheon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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
        this.service = new ThreadPoolExecutor(workers, workers, 0L,
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
            trySendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        if (myself != null) {
            final var resp = processLocalRequest(key, request);
            try {
                trySendResponse(session, resp.get());
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
            trySendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        sendToReplicas(key, replicas, session, request);
    }

    private void sendToReplicas(final @Param(value = "id", required = true) String key,
                                @NotNull final Replicas replicas,
                                @NotNull final HttpSession session,
                                final @Param("request") Request request) {
        final var responsibleNodes = sharding.getResponsibleNodes(key, replicas);
        final var answers = new CompletableFuture<?>[responsibleNodes.size()];
        int i = 0;
        for (final var node : responsibleNodes) {
            if (sharding.isMe(node)) {
                answers[i] = processLocalRequest(key, request);
            } else {
                answers[i] = sharding.passOn(node, addMyselfParamToRequest(request));
            }
            i++;
        }
        final var all = CompletableFuture.allOf(answers);

        all.thenAccept(ignored -> {
            final var composer = new ReplicasResponseComposer(replicas.getAckCount());
            for (final var answer : answers) {
                final Response response;
                try {
                    response = (Response) answer.get();
                } catch (InterruptedException | ExecutionException e) {
                    continue;
                }
                composer.addResponse(response);
            }
            final var requiredResponse = composer.getComposedResponse();
            trySendResponse(session, requiredResponse);
        }).exceptionally((ex) -> {
            logger.error(SERV_UN, ex);
            trySendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return null;
        });
    }

    private CompletableFuture<Response> processLocalRequest(final @Param(value = "id", required = true) String key,
                                                            final @Param("request") Request request) {
        return CompletableFuture.supplyAsync(() -> {
            switch (request.getMethod()) {
                case METHOD_GET:
                    return get(key);
                case METHOD_PUT:
                    return put(key, request);
                case METHOD_DELETE:
                    return delete(key);
                default:
                    return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }, service);
    }

    private Response get(final String key) {
        try {
            final var record = Record.newFromDAO(dao, key);
            return Response.ok(record.getRawValue());
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.getInternal() method; internal error: ", ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response put(final String key,
                     final Request request) {
        try {
            final var record = Record.newRecord(key, request.getBody());
            record.save(dao);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.putInternal() method; internal error: ", ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response delete(final String key) {
        try {
            final var record = Record.newRemoved(key);
            record.save(dao);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.delete() method; internal error: ", ex);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
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
        trySendResponse(session, new Response(Response.OK, Response.EMPTY));
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        trySendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY));
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
