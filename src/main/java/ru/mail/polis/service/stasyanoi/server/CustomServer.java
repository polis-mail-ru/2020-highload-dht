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
import ru.mail.polis.service.stasyanoi.CustomExecutor;
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;
import ru.mail.polis.service.stasyanoi.server.internal.ConstantsServer;

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
import java.util.concurrent.TimeUnit;

public class CustomServer extends ConstantsServer {

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
            Util.send503Error(session);
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
            executorService.execute(() -> getRepInternal(idParam, session));
        } catch (RejectedExecutionException e) {
            Util.send503Error(session);
        }
    }

    private void getRepInternal(final String idParam, final HttpSession session) {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer id = Util.getKey(idParam);
            responseHttp = getResponseIfIdNotNull(id, dao);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getInternal(final String idParam, final HttpSession session, final Request request) {
        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final ByteBuffer id = Mapper.fromBytes(idArray);
            final Request noRepRequest = getNoRepRequest(request, super.port);
            final Response responseHttpCurrent = getProxy(noRepRequest, node, id);
            tempNodeMapping.remove(node);
            responseHttp = getReplicaGetResponse(request,
                    tempNodeMapping, responseHttpCurrent, nodeMapping, super.port);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Response getProxy(final Request request, final int node, final ByteBuffer id) {
        final Response responseHttp;
        if (node == nodeNum) {
            responseHttp = getResponseIfIdNotNull(id, dao);
        } else {
            responseHttp = routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
    }

    /**
     * Get replica request for GET.
     *
     * @param request - request to replicate.
     * @param tempNodeMapping - node mapping for replication
     * @param responseHttpCurrent - this server get response.
     * @param nodeMapping - nodes
     * @param port - this server port.
     * @return - the replica response.
     */
    private Response getReplicaGetResponse(final Request request,
                                          final Map<Integer, String> tempNodeMapping,
                                          final Response responseHttpCurrent,
                                          final Map<Integer, String> nodeMapping,
                                          final int port) {
        final Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = Util.ackFromPair(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                    tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = Merger.mergeGetResponses(responses, ack, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
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
            Util.send503Error(session);
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
            Util.send503Error(session);
        }
    }

    private void putRepInternal(final String idParam, final Request request, final HttpSession session) {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = putHere(request, Util.getKey(idParam));
        }
        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void putInternal(final String idParam, final Request request, final HttpSession session) {

        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final Request noRepRequest = getNoRepRequest(request, super.port);
            final Response responseHttpCurrent = putProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            final Pair<Map<Integer, String>, Map<Integer, String>> mappings = new Pair<>(tempNodeMapping, nodeMapping);
            responseHttp = getPutReplicaResponse(request, mappings, responseHttpCurrent, super.port);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Response putProxy(final Request request, final byte[] idArray, final int node) {
        Response responseHttp;
        if (node == nodeNum) {
            responseHttp = putHere(request, Mapper.fromBytes(idArray));
        } else {
            responseHttp = routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
    }

    @NotNull
    private Response putHere(final Request request, final ByteBuffer key) {
        Response responseHttp;
        final ByteBuffer value = Util.getByteBufferValue(request);
        try {
            dao.upsert(key, value);
            responseHttp = Util.responseWithNoBody(Response.CREATED);
        } catch (IOException e) {
            responseHttp = Util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
        return responseHttp;
    }

    /**
     * Get put replicas.
     *
     * @param request - request to replicate.
     * @param mappings - nodes that can have the replicas and the total amount nodes .
     * @param responseHttpCurrent - this server responseto request.
     * @param port - this server port.
     * @return - returned response.
     */
    private Response getPutReplicaResponse(final Request request,
                                          final Pair<Map<Integer, String>, Map<Integer, String>> mappings,
                                          final Response responseHttpCurrent,
                                          final int port) {
        final Map<Integer, String> tempNodeMapping = mappings.getValue0();
        final Map<Integer, String> nodeMapping = mappings.getValue1();
        Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = Util.ackFromPair(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                    tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = Merger.mergePutDeleteResponses(responses, ack, 201, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
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
            Util.send503Error(session);
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
            Util.send503Error(session);
        }
    }

    private void deleteRepInternal(final String idParam, final HttpSession session) {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer key = Util.getKey(idParam);
            try {
                dao.remove(key);
                responseHttp = Util.responseWithNoBody(Response.ACCEPTED);
            } catch (IOException e) {
                responseHttp = Util.responseWithNoBody(Response.INTERNAL_ERROR);
            }
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
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final Request noRepRequest = getNoRepRequest(request, super.port);
            final Response responseHttpCurrent = deleteProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            responseHttp = getDeleteReplicaResponse(request, tempNodeMapping,
                    responseHttpCurrent, nodeMapping, super.port);
        }

        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private Response deleteProxy(final Request request, final byte[] idArray, final int node) {
        Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            try {
                dao.remove(key);
                responseHttp = Util.responseWithNoBody(Response.ACCEPTED);
            } catch (IOException e) {
                responseHttp = Util.responseWithNoBody(Response.INTERNAL_ERROR);
            }
        } else {
            responseHttp = routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
    }

    /**
     * Get response for delete replication.
     *
     * @param request - request to replicate.
     * @param tempNodeMapping - nodes that can get replicas.
     * @param responseHttpCurrent - this server response.
     * @param nodeMapping - nodes
     * @param port - this server port.
     * @return - response for delete replicating.
     */
    private Response getDeleteReplicaResponse(final Request request,
                                              final Map<Integer, String> tempNodeMapping,
                                              final Response responseHttpCurrent,
                                              final Map<Integer, String> nodeMapping,
                                              final int port) {
        final Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = getRF(request, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                    tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = Merger.mergePutDeleteResponses(responses, ack, 202, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }

    /**
     * Ger replicas.
     *
     * @param responseHttpTemp - current server response.
     * @param tempNodeMapping - nodes for potential replication
     * @param from - RF replicas ack from
     * @param request - request to replicate
     * @param port - this server port.
     * @return - list of replica responses
     */
    private List<Response> getResponsesFromReplicas(final Response responseHttpTemp,
                                                   final Map<Integer, String> tempNodeMapping,
                                                   final int from,
                                                   final Request request,
                                                   final int port) {
        final List<Response> responses = new CopyOnWriteArrayList<>();
        final var completableFutures = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>(
                        new Pair<>(asyncHttpClient, nodeHost.getValue()),
                        getNewRequest(request, port)))
                .map(clientRequest -> {
                    final Pair<HttpClient, String> clientAndHost = clientRequest.getValue0();
                    final HttpClient client = clientAndHost.getValue0();
                    final String host = clientAndHost.getValue1();
                    final Request oneNioRequest = clientRequest.getValue1();
                    final HttpRequest javaRequest = Util.getJavaRequest(oneNioRequest, host);
                    return client.sendAsync(javaRequest, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApplyAsync(Util::getOneNioResponse)
                            .handle((response, throwable) -> {
                                if (throwable == null) {
                                    return response;
                                } else {
                                    return Util.responseWithNoBody(Response.INTERNAL_ERROR);
                                }
                            })
                            .thenAcceptAsync(responses::add);
                })
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).join();
        responses.add(responseHttpTemp);
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
    private Request getNewRequest(final Request request, final int port) {
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
     * Get repsponse for get request.
     *
     * @param id - key.
     * @param dao - dao to use.
     * @return - response.
     */
    private Response getResponseIfIdNotNull(final ByteBuffer id, final DAO dao) {
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            final Pair<byte[], byte[]> bodyTimestamp = Util.getTimestamp(bytes);
            final byte[] newBody = bodyTimestamp.getValue0();
            final byte[] time = bodyTimestamp.getValue1();
            final Response okResponse = Response.ok(newBody);
            Util.addTimestampHeader(time, okResponse);
            return okResponse;
        } catch (NoSuchElementException | IOException e) {
            final byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return Util.responseWithNoBody(Response.NOT_FOUND);
            } else {
                final Response deletedResponse = Util.responseWithNoBody(Response.NOT_FOUND);
                Util.addTimestampHeader(deleteTime, deletedResponse);
                return deletedResponse;
            }
        }
    }

    @Override
    public synchronized void start() {
        logger.info("start " + nodeNum);
        super.start();
        executorService = CustomExecutor.getExecutor();
        dao.open();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        logger.info("stop " + nodeNum);
        try {
            dao.close();
            executorService.shutdown();
            executorService.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Default handler for unmapped requests.
     *
     * @param request - unmapped request
     * @param session - session object
     * @throws IOException - if input|output exceptions occur within the method
     */
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = Util.responseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }

    /**
     * Hash route request.
     *
     * @param request - request to route.
     * @param node - node to route the request to.
     * @param nodeMapping - node list.
     * @return - returned response.
     */
    private Response routeRequest(final Request request, final int node, final Map<Integer, String> nodeMapping) {
        try {
            return Util.getOneNioResponse(asyncHttpClient.send(Util.getJavaRequest(request,nodeMapping.get(node)),
                    HttpResponse.BodyHandlers.ofByteArray()));
        } catch (InterruptedException | IOException e) {
            return Util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    @NotNull
    private Pair<Integer, Integer> getRF(final Request request, final Map<Integer, String> nodeMapping) {
        return Util.ackFromPair(request, replicationDefaults, nodeMapping);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response status() {
        return Util.responseWithNoBody(Response.OK);
    }
}
