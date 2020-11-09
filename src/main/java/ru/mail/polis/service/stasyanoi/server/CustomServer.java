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
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;
import ru.mail.polis.service.stasyanoi.server.internal.OverridedServer;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import static ru.mail.polis.service.stasyanoi.Util.*;

public class CustomServer extends OverridedServer {

    /**
     * Create custom server.
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
            executorService.execute(() -> getInternal(idParam, session, request));
        } catch (RejectedExecutionException e) {
            send503Error(session);
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
    public void getRep(final @Param("id") String idParam, final HttpSession session) {
        try {
            executorService.execute(() -> getReplicationInternal(idParam, session));
        } catch (RejectedExecutionException e) {
            send503Error(session);
        }
    }

    private void getReplicationInternal(final String idParam, final HttpSession session) {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = getResponseFromLocalNode(idParam, dao);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getInternal(final String idParam, final HttpSession session, final Request request) {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final int node = getNode(idParam, nodeAmmount);
            final Response responseHttpTemp;
            if (node == thisNodeIndex) {
                responseHttpTemp = getResponseFromLocalNode(idParam, dao);
            } else {
                responseHttpTemp = routeRequestToRemoteNode(getNoRepRequest(request, super.port), node, nodeMapping);
            }

            if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
                final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
                tempNodeMapping.remove(node);
                final Pair<Integer, Integer> ackFrom = getRF(request, nodeMapping);
                final int from = ackFrom.getValue1();
                final List<Response> responses = getResponsesFromReplicas(tempNodeMapping, from - 1, request, port);
                final Integer ack = ackFrom.getValue0();
                responses.add(responseHttpTemp);
                responseHttp = Merger.mergeGetResponses(responses, ack, nodeMapping);
            } else {
                responseHttp = responseHttpTemp;
            };
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Get repsponse for get request.
     *
     * @param dao - dao to use.
     * @return - response.
     */
    private Response getResponseFromLocalNode(String idParam, final DAO dao) {
        ByteBuffer id = Mapper.fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            final Pair<byte[], byte[]> bodyTimestamp = getTimestamp(bytes);
            final byte[] newBody = bodyTimestamp.getValue0();
            final byte[] time = bodyTimestamp.getValue1();
            final Response okResponse = Response.ok(newBody);
            addTimestampHeader(time, okResponse);
            return okResponse;
        } catch (NoSuchElementException | IOException e) {
            final byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return responseWithNoBody(Response.NOT_FOUND);
            } else {
                final Response deletedResponse = responseWithNoBody(Response.NOT_FOUND);
                addTimestampHeader(deleteTime, deletedResponse);
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
            executorService.execute(() -> putInternal(idParam, request, session));
        } catch (RejectedExecutionException e) {
            send503Error(session);
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
    public void putRep(final @Param("id") String idParam, final Request request, final HttpSession session) {
        try {
            executorService.execute(() -> putRepInternal(idParam, request, session));
        } catch (RejectedExecutionException e) {
            send503Error(session);
        }
    }

    private void putRepInternal(final String idParam, final Request request, final HttpSession session) {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = putIntoLocalNode(request, idParam);
        }
        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void putInternal(final String idParam, final Request request, final HttpSession session) {

        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final int node = getNode(idParam, nodeAmmount);
            Response responseHttpTemp;
            if (node == thisNodeIndex) {
                responseHttpTemp = putIntoLocalNode(request, idParam);
            } else {
                responseHttpTemp = routeRequestToRemoteNode(getNoRepRequest(request, super.port), node, nodeMapping);
            }

            if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
                final Pair<Integer, Integer> ackFrom = getRF(request, nodeMapping);
                final int from = ackFrom.getValue1();
                final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
                tempNodeMapping.remove(node);
                final List<Response> responses = getResponsesFromReplicas(tempNodeMapping, from - 1, request, port);
                final Integer ack = ackFrom.getValue0();
                responses.add(responseHttpTemp);
                responseHttp = Merger.mergePutDeleteResponses(responses, ack, 201, nodeMapping);
            } else {
                responseHttp = responseHttpTemp;
            }
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @NotNull
    private Response putIntoLocalNode(final Request request, final String keyString) {
        Response responseHttp;
        try {
            dao.upsert(getKey(keyString), getByteBufferValue(request));
            responseHttp = responseWithNoBody(Response.CREATED);
        } catch (IOException e) {
            responseHttp = responseWithNoBody(Response.INTERNAL_ERROR);
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
            executorService.execute(() -> deleteInternal(idParam, request, session));
        } catch (RejectedExecutionException e) {
            send503Error(session);
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
    public void deleteRep(final @Param("id") String idParam, final HttpSession session) {
        try {
            executorService.execute(() -> deleteRepInternal(idParam, session));
        } catch (RejectedExecutionException e) {
            send503Error(session);
        }
    }

    private void deleteRepInternal(final String idParam, final HttpSession session) {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = deleteInLocalNode(idParam);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private void deleteInternal(final String idParam, final Request request, final HttpSession session) {
        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final int node = getNode(idParam, nodeAmmount);
            Response responseHttpTemp;
            if (node == thisNodeIndex) {
                responseHttpTemp = deleteInLocalNode(idParam);
            } else {
                responseHttpTemp = routeRequestToRemoteNode(getNoRepRequest(request, super.port), node, nodeMapping);
            }
            tempNodeMapping.remove(node);

            if (request.getParameter(SHOULD_REPLICATE, TRUE).equals(TRUE)) {
                final Pair<Integer, Integer> ackFrom = getRF(request, nodeMapping);
                final int from = ackFrom.getValue1();
                final List<Response> responses = getResponsesFromReplicas(tempNodeMapping, from - 1, request, port);
                final Integer ack = ackFrom.getValue0();
                responses.add(responseHttpTemp);
                responseHttp = Merger.mergePutDeleteResponses(responses, ack, 202, nodeMapping);
            } else {
                responseHttp = responseHttpTemp;
            }
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    @NotNull
    private Response deleteInLocalNode(String idParam) {
        Response responseHttp;
        final ByteBuffer key = getKey(idParam);
        try {
            dao.remove(key);
            responseHttp = responseWithNoBody(Response.ACCEPTED);
        } catch (IOException e) {
            responseHttp = responseWithNoBody(Response.INTERNAL_ERROR);
        }
        return responseHttp;
    }

    /**
     * Ger replicas.
     *
     * @param tempNodeMapping - nodes for potential replication
     * @param from - RF replicas ack from
     * @param request - request to replicate
     * @param port - this server port.
     * @return - list of replica responses
     */
    private List<Response> getResponsesFromReplicas(final Map<Integer, String> tempNodeMapping,
                                                   final int from,
                                                   final Request request,
                                                   final int port) {
        final List<Response> responses = new CopyOnWriteArrayList<>();
        final var completableFutures = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>(
                        new Pair<>(asyncHttpClient, nodeHost.getValue()),
                        getNewReplicationRequest(request, port)))
                .map(clientRequest -> {
                    final Pair<HttpClient, String> clientAndHost = clientRequest.getValue0();
                    final HttpClient client = clientAndHost.getValue0();
                    final String host = clientAndHost.getValue1();
                    final Request oneNioRequest = clientRequest.getValue1();
                    final HttpRequest javaRequest = getJavaRequest(oneNioRequest, host);
                    return client.sendAsync(javaRequest, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApplyAsync(Util::getOneNioResponse)
                            .handle((response, throwable) -> {
                                if (throwable == null) {
                                    return response;
                                } else {
                                    return responseWithNoBody(Response.INTERNAL_ERROR);
                                }
                            })
                            .thenAcceptAsync(responses::add);
                })
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).join();
        return responses;
    }

    /**
     * Create new request.
     *
     * @param request - old request.
     * @param port - this server port.
     * @return - new Request.
     */
    @NotNull
    private Request getNewReplicationRequest(final Request request, final int port) {
        final String path = request.getPath();
        final String queryString = request.getQueryString();
        final String newPath = path + "/rep?" + queryString;
        final Request requestNew = getCloneRequest(request, newPath, port);
        requestNew.setBody(request.getBody());
        return requestNew;
    }

    /**
     * Create no replication request.
     *
     * @param request - old request.
     * @param port - this server port.
     * @return - new request.
     */
    @NotNull
    private Request getNoRepRequest(final Request request,
                                   final int port) {
        final String path = request.getPath();
        final String queryString = request.getQueryString();
        final String newPath;
        if (request.getQueryString().contains("&reps=false")) {
            newPath = path + "?" + queryString;
        } else {
            newPath = path + "?" + queryString + "&reps=false";
        }
        final Request noRepRequest = getCloneRequest(request, newPath, port);
        noRepRequest.setBody(request.getBody());
        return noRepRequest;
    }

    @NotNull
    private Request getCloneRequest(final Request request, final String newPath, final int thisServerPort) {
        final Request noRepRequest = new Request(request.getMethod(), newPath, true);
        Arrays.stream(request.getHeaders())
                .filter(Objects::nonNull)
                .filter(header -> !header.contains("Host: "))
                .forEach(noRepRequest::addHeader);
        noRepRequest.addHeader("Host: localhost:" + thisServerPort);
        return noRepRequest;
    }

    /**
     * Hash route request.
     *
     * @param request - request to route.
     * @param node - node to route the request to.
     * @param nodeMapping - node list.
     * @return - returned response.
     */
    private Response routeRequestToRemoteNode(final Request request, final int node, final Map<Integer, String> nodeMapping) {
        try {
            return getOneNioResponse(asyncHttpClient.send(getJavaRequest(request,nodeMapping.get(node)),
                    HttpResponse.BodyHandlers.ofByteArray()));
        } catch (InterruptedException | IOException e) {
            return responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    @NotNull
    private Pair<Integer, Integer> getRF(final Request request, final Map<Integer, String> nodeMapping) {
        return ackFromPair(request, replicationDefaults, nodeMapping);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response status() {
        return responseWithNoBody(Response.OK);
    }
}
