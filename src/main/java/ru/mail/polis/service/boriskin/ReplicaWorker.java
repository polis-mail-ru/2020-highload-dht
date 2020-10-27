package ru.mail.polis.service.boriskin;

import com.google.common.base.Charsets;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.boriskin.NewDAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

final class ReplicaWorker {
    private static final Logger logger = LoggerFactory.getLogger(ReplicaWorker.class);

    static final String PROXY_HEADER = "X-OK-Proxy: true";

    @NotNull
    private final CompletionService<Response> proxyService;
    @NotNull
    private final NewDAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> nodeToClientMap;

    ReplicaWorker(
            @NotNull final ExecutorService proxyWorkers,
            @NotNull final DAO dao,
            @NotNull final Topology<String> topology) {
        this.proxyService = new ExecutorCompletionService<>(proxyWorkers);
        this.dao = (NewDAO) dao;
        this.topology = topology;
        this.nodeToClientMap = new HashMap<>();
        for (final String node : topology.all()) {
            if (topology.isMyNode(node)) {
                continue;
            }
            nodeToClientMap.put(
                    node,
                    new HttpClient(new ConnectionString(node + "?timeout=100")));
        }
    }

    @NotNull
    Response entity(
            @NotNull final MetaInfoRequest mir,
            @NotNull final Request request) {
        if (mir.isAlreadyProxied()) {
            return doFinalOperation(mir, request);
        }

        final List<String> replicas =
                topology.replicas(
                        ByteBuffer.wrap(
                                mir.getId().getBytes(Charsets.UTF_8)),
                        mir.getReplicaFactor().getFrom()
                );

        final ArrayList<Value> values = new ArrayList<>();
        int acks = 0;
        if (replicas.contains(topology.recogniseMyself())) {
            try {
                acks = doIfMyNodeIsInReplicas(mir, request, values);
            } catch (IllegalStateException illegalStateException) {
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        }

        for (final Response response : getResponses(replicas, mir)) {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    try {
                        if (response.getStatus() == 404) {
                            acks++;
                            return isUnreachable(acks, mir)
                                    ? new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY) : response;
                        } else {
                            values.add(Value.from(response));
                            acks++;
                            break;
                        }
                    } catch (IllegalArgumentException illegalArgumentException) {
                        logger.error("Нода: {}. Непонятный запрос ",
                                topology.recogniseMyself(), illegalArgumentException);
                        return new Response(Response.BAD_REQUEST, Response.EMPTY);
                    }
                case Request.METHOD_PUT:
                    if (response.getStatus() == 201) {
                        acks++;
                    }
                    break;
                case Request.METHOD_DELETE:
                    if (response.getStatus() == 202) {
                        acks++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Неверный запрос");
            }
        }

        if (isUnreachable(acks, mir)) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        } else {
            return doIfNotUnreachable(request, values);
        }
    }

    private Response doIfNotUnreachable(
            @NotNull final Request request,
            final ArrayList<Value> values) {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return Value.transform(Value.merge(values), false);
            case Request.METHOD_PUT:
                return new Response(Response.CREATED, Response.EMPTY);
            case Request.METHOD_DELETE:
                return new Response(Response.ACCEPTED, Response.EMPTY);
            default:
                throw new IllegalStateException("Неверный запрос");
        }
    }

    private int doIfMyNodeIsInReplicas(
            @NotNull final MetaInfoRequest mir,
            @NotNull final Request request,
            final ArrayList<Value> values) {
        int acks = 0;
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    values.add(
                            Value.from(
                                    dao.getTableCell(
                                            ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)))));
                    break;
                case Request.METHOD_PUT:
                    dao.upsert(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)), mir.getValue());
                    break;
                case Request.METHOD_DELETE:
                    dao.remove(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)));
                    break;
                default:
                    throw new IllegalStateException("Неверный запрос");
            }
            acks++;
            return acks;
        } catch (IOException ioException) {
            logger.error("Нода: {}. Ошибка при {} в {} : ",
                    topology.recogniseMyself(), request, mir.getId(), ioException);
            return acks;
        }
    }

    private Response doFinalOperation(
            @NotNull final MetaInfoRequest mir,
            @NotNull final Request request) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    return Value.transform(
                            Value.from(
                                    dao.getTableCell(
                                            ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)))),
                            true) ;
                case Request.METHOD_PUT:
                    dao.upsert(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)), mir.getValue());
                    return new Response(Response.CREATED, Response.EMPTY);
                case Request.METHOD_DELETE:
                    dao.remove(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)));
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ioException) {
            logger.error("Ошибка: ", ioException);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private boolean isUnreachable(
            final int acks,
            @NotNull final MetaInfoRequest meta) {
        return acks < meta.getReplicaFactor().getAck();
    }

    @NotNull
    private List<Response> getResponses(
            @NotNull final List<String> replicas,
            @NotNull final MetaInfoRequest mir) {
        for (final String node: replicas) {
            if (!topology.isMyNode(node)) {
                proxyService.submit(() -> proxy(node, mir.getRequest()));
            }
        }

        final int responsesSize =
                replicas.contains(topology.recogniseMyself())
                ? replicas.size() - 1 : replicas.size();

        final ArrayList<Response> responses = new ArrayList<>();
        for (int i = 0; i < responsesSize; i++){
            try {
                responses.add(proxyService.take().get());
            } catch (ExecutionException | InterruptedException exception) {
                logger.error("Нода: {}. Ошибка при проксировании ",
                        topology.recogniseMyself(), exception);
            }
        }

        return responses;
    }

    @NotNull
    private Response proxy(
            @NotNull final String node,
            @NotNull final Request request) throws IOException {
        try {
            request.addHeader(PROXY_HEADER);
            return nodeToClientMap.get(node).invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            throw new IOException("Не получилось проксировать запрос ", exception);
        }
    }
}
