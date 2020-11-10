package ru.mail.polis.service.stasyanoi.server;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;
import ru.mail.polis.service.stasyanoi.server.helpers.AckFrom;
import ru.mail.polis.service.stasyanoi.server.helpers.Arguments;
import ru.mail.polis.service.stasyanoi.server.internal.BaseFunctionalityServer;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

public class CustomServer extends BaseFunctionalityServer {

    /**
     * Custom server.
     *
     * @param dao - DAO to use.
     * @param config - config for server.
     * @param topology - topology of services.
     * @throws IOException - if an IO exception occurs.
     */
    public CustomServer(final DAO dao, final HttpServerConfig config, final Set<String> topology) throws IOException {
        super(dao, config, topology);
    }

    /**
     * Get a record by key.
     *
     * @param idParam - key.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(final @Param("id") String idParam, final HttpSession session, final Request request) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> getResponseFromLocalAndReplicas(idParam, request)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    /**
     * Endpoint for get replication.
     *
     * @param idParam - key.
     * @param session - session for the request.
     */
    @Path("/v0/entity/rep")
    @RequestMethod(Request.METHOD_GET)
    public void getReplication(final @Param("id") String idParam, final HttpSession session) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> getResponseFromLocalNode(idParam)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    private Response getResponseFromLocalAndReplicas(final String idParam, final Request request) {
        final Response responseHttp;
        final int node = util.getNode(idParam, nodeAmount);
        final Response responseHttpTemp;
        if (node == thisNodeIndex) {
            responseHttpTemp = getResponseFromLocalNode(idParam);
        } else {
            responseHttpTemp = routeRequestToRemoteNode(util.getNoRepRequest(request, super.port), node,
                    nodeIndexToUrlMapping);
        }
        if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
            responseHttp = replicateGet(request, node, responseHttpTemp);
        } else {
            responseHttp = responseHttpTemp;
        }
        return responseHttp;
    }

    private Response replicateGet(final Request request, final int node, final Response responseHttpTemp) {
        final Response responseHttp;
        final AckFrom ackFrom = util.getRF(request.getParameter(REPLICAS), nodeIndexToUrlMapping.size());
        final List<Response> responses = getReplicaResponses(request, node, ackFrom.getFrom() - 1);
        responses.add(responseHttpTemp);
        responseHttp = merger.mergeGetResponses(responses, ackFrom.getAck(), nodeIndexToUrlMapping);
        return responseHttp;
    }

    private Response getResponseFromLocalNode(final String idParam) {
        final ByteBuffer id = Mapper.fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            final Pair<byte[], byte[]> bodyTimestamp = util.getTimestamp(bytes);
            final byte[] newBody = bodyTimestamp.getValue0();
            final byte[] time = bodyTimestamp.getValue1();
            final Response okResponse = Response.ok(newBody);
            util.addTimestampHeader(time, okResponse);
            return okResponse;
        } catch (NoSuchElementException | IOException e) {
            final byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return util.responseWithNoBody(Response.NOT_FOUND);
            } else {
                final Response deletedResponse = util.responseWithNoBody(Response.NOT_FOUND);
                util.addTimestampHeader(deleteTime, deletedResponse);
                return deletedResponse;
            }
        }
    }

    /**
     * Create or update a record.
     *
     * @param idParam - key of the record.
     * @param request - request with the body.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(final @Param("id") String idParam, final Request request, final HttpSession session) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> putResponseFromLocalAndReplicas(idParam, request)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    /**
     * Put replication.
     *
     * @param idParam - key.
     * @param request - out request.
     * @param session - session.
     */
    @Path("/v0/entity/rep")
    @RequestMethod(Request.METHOD_PUT)
    public void putReplication(final @Param("id") String idParam, final Request request, final HttpSession session) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> putIntoLocalNode(request, idParam)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    private Response putResponseFromLocalAndReplicas(final String idParam, final Request request) {
        Response responseHttp;
        final int node = util.getNode(idParam, nodeAmount);
        Response responseHttpTemp;
        if (node == thisNodeIndex) {
            responseHttpTemp = putIntoLocalNode(request, idParam);
        } else {
            responseHttpTemp = routeRequestToRemoteNode(util.getNoRepRequest(request, super.port), node,
                    nodeIndexToUrlMapping);
        }
        if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
            responseHttp = replicatePutOrDelete(request, node, responseHttpTemp, 201);
        } else {
            responseHttp = responseHttpTemp;
        }
        return responseHttp;
    }

    private Response replicatePutOrDelete(final Request request, final int node, final Response responseHttpTemp,
                                          final int i) {
        Response responseHttp;
        final AckFrom ackFrom = util.getRF(request.getParameter(REPLICAS), nodeIndexToUrlMapping.size());
        final List<Response> responses = getReplicaResponses(request, node, ackFrom.getFrom() - 1);
        responses.add(responseHttpTemp);
        responseHttp = merger.mergePutDeleteResponses(responses, ackFrom.getAck(), i, nodeIndexToUrlMapping);
        return responseHttp;
    }

    @NotNull
    private List<Response> getReplicaResponses(final Request request, final int node, final int fromOtherReplicas) {
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeIndexToUrlMapping);
        tempNodeMapping.remove(node);
        return getResponsesFromReplicas(tempNodeMapping, fromOtherReplicas, request, port);
    }

    @NotNull
    private Response putIntoLocalNode(final Request request, final String keyString) {
        Response responseHttp;
        try {
            dao.upsert(util.getKey(keyString), util.getByteBufferValue(request));
            responseHttp = util.responseWithNoBody(Response.CREATED);
        } catch (IOException e) {
            responseHttp = util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
        return responseHttp;
    }

    /**
     * Delete a record.
     *
     * @param idParam - key of the record to delete.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(final @Param("id") String idParam, final Request request, final HttpSession session) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> deleteResponseFromLocalAndReplicas(idParam, request)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    /**
     * Delete replication.
     *
     * @param idParam - key.
     * @param session - session.
     */
    @Path("/v0/entity/rep")
    @RequestMethod(Request.METHOD_DELETE)
    public void deleteReplication(final @Param("id") String idParam, final HttpSession session) {
        try {
            executorService.execute(() -> internalRun(new Arguments(idParam, session),
                    () -> deleteInLocalNode(idParam)));
        } catch (RejectedExecutionException e) {
            util.send503Error(session);
        }
    }

    private void internalRun(final Arguments replicationArguments, final Supplier<Response> responseSupplier) {
        Response responseHttp;
        final String idParam = replicationArguments.getIdParam();
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = responseSupplier.get();
        }
        try {
            replicationArguments.getSession().sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private Response deleteResponseFromLocalAndReplicas(final String idParam, final Request request) {
        final int node = util.getNode(idParam, nodeAmount);
        Response responseHttpTemp;
        if (node == thisNodeIndex) {
            responseHttpTemp = deleteInLocalNode(idParam);
        } else {
            responseHttpTemp = routeRequestToRemoteNode(util.getNoRepRequest(request, super.port), node,
                    nodeIndexToUrlMapping);
        }
        Response responseHttp;
        if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
            responseHttp = replicatePutOrDelete(request, node, responseHttpTemp, 202);
        } else {
            responseHttp = responseHttpTemp;
        }
        return responseHttp;
    }

    @NotNull
    private Response deleteInLocalNode(final String idParam) {
        Response responseHttp;
        final ByteBuffer key = util.getKey(idParam);
        try {
            dao.remove(key);
            responseHttp = util.responseWithNoBody(Response.ACCEPTED);
        } catch (IOException e) {
            responseHttp = util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
        return responseHttp;
    }

    private List<Response> getResponsesFromReplicas(final Map<Integer, String> tempNodeMapping, final int from,
                                                   final Request request,
                                                   final int port) {
        final List<Response> responses = new CopyOnWriteArrayList<>();
        final var completableFutures = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> asyncHttpClient.sendAsync(
                        util.getJavaRequest(util.getNewReplicationRequest(request, port), nodeHost.getValue()),
                        HttpResponse.BodyHandlers.ofByteArray()).thenApplyAsync(util::getOneNioResponse)
                        .handleAsync(util::filterResponse).thenAcceptAsync(responses::add))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).join();
        return responses;
    }

    private Response routeRequestToRemoteNode(final Request request, final int node,
                                              final Map<Integer, String> nodeMapping) {
        try {
            return util.getOneNioResponse(asyncHttpClient.send(util.getJavaRequest(request,nodeMapping.get(node)),
                    HttpResponse.BodyHandlers.ofByteArray()));
        } catch (InterruptedException | IOException e) {
            return util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }
}
