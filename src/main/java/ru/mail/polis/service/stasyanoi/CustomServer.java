package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpServer;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.mail.polis.service.stasyanoi.Merger.getEndResponseGet;
import static ru.mail.polis.service.stasyanoi.Merger.getEndResponsePutAndDelete;

public class CustomServer extends HttpServer {
    private final Map<Integer, String> nodeMapping;
    private final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");
    private final int nodeCount;
    private static final String TRUE_VAL = "true";
    private static final String REPS = "reps";
    private int nodeNum;
    private final DAO dao;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Create custom server.
     *
     * @param dao - DAO to use.
     * @param config - config for server.
     * @param topology - topology of services.
     * @throws IOException - if an IO exception occurs.
     */
    public CustomServer(final DAO dao,
                        final HttpServerConfig config,
                        final Set<String> topology) throws IOException {
        super(config);
        this.nodeCount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();
        for (int i = 0; i < urls.size(); i++) {
            nodeMappingTemp.put(i, urls.get(i));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
        }
        this.nodeMapping = nodeMappingTemp;
        this.dao = dao;
    }


    /**
     * Get a record by key.
     *
     * @param idParam - key.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(final @Param("id") String idParam,
                    final HttpSession session,
                    final Request request) {
        executorService.execute(() -> {
            try {
                getInternal(idParam, session, request);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
    }

    /**
     * Endpoint for get replication.
     *
     * @param idParam - key.
     * @param session - session for the request.
     */
    @Path("/v0/entity/rep")
    @RequestMethod(Request.METHOD_GET)
    public void getRep(final @Param("id") String idParam,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                getRepInternal(idParam, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
    }

    private void getRepInternal(final String idParam,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final ByteBuffer id = Mapper.fromBytes(idArray);
            responseHttp = getResponseIfIdNotNull(id);
        }
        session.sendResponse(responseHttp);
    }

    private void getInternal(final String idParam,
                             final HttpSession session,
                             final Request request) throws IOException {
        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = getResponseGet(idParam, request, tempNodeMapping, replicationDefaults);
        }
        session.sendResponse(responseHttp);
    }

    private Response getResponseGet(final String idParam,
                                    final Request request,
                                    final Map<Integer, String> tempNodeMapping,
                                    final List<String> replicationDefaults) throws IOException {
        final Response responseHttp;
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int node = Util.getNode(idArray, nodeCount);
        final ByteBuffer id = Mapper.fromBytes(idArray);
        final Request noRepRequest = GetHelper.getNoRepRequest(request, super.port);
        final Response responseHttpCurrent = getProxy(noRepRequest, node, id);
        tempNodeMapping.remove(node);
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom =
                    Util.getAckFrom(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses =
                    GetHelper.getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request, super.port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = getEndResponseGet(responses, ack, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }

    private Response getProxy(final Request request,
                              final int node,
                              final ByteBuffer id) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            responseHttp = getResponseIfIdNotNull(id);
        } else {
            responseHttp = Util.routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
    }

    @NotNull
    private Response getResponseIfIdNotNull(final ByteBuffer id) throws IOException {
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            final Pair<byte[], byte[]> bodyTimestamp = Util.getTimestamp(bytes);
            final byte[] newBody = bodyTimestamp.getValue0();
            final byte[] time = bodyTimestamp.getValue1();
            final Response ok = Response.ok(newBody);
            Util.addTimestampHeader(time, ok);
            return ok;
        } catch (NoSuchElementException e) {
            final byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return Util.getResponseWithNoBody(Response.NOT_FOUND);
            } else {
                final Response deletedResponse = Util.getResponseWithNoBody(Response.NOT_FOUND);
                Util.addTimestampHeader(deleteTime, deletedResponse);
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
    public void put(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putInternal(idParam, request, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
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
    public void putRep(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putRepInternal(idParam, request, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
    }

    private void putRepInternal(final String idParam,
                                final Request request,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final ByteBuffer key = Mapper.fromBytes(idArray);
            byte[] body = request.getBody();
            body = Util.addTimestamp(body);
            final ByteBuffer value = Mapper.fromBytes(body);
            dao.upsert(key, value);
            responseHttp = Util.getResponseWithNoBody(Response.CREATED);
        }
        session.sendResponse(responseHttp);
    }

    private void putInternal(final String idParam,
                             final Request request,
                             final HttpSession session) throws IOException {

        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final Request noRepRequest = GetHelper.getNoRepRequest(request, super.port);
            final Response responseHttpCurrent = putProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
                final Pair<Integer, Integer> ackFrom =
                        Util.getAckFrom(request, replicationDefaults, nodeMapping);
                final int from = ackFrom.getValue1();
                final List<Response> responses =
                        GetHelper.getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request, super.port);
                final Integer ack = ackFrom.getValue0();
                responseHttp = getEndResponsePutAndDelete(responses, ack, 201, nodeMapping);
            } else {
                responseHttp = responseHttpCurrent;
            }
        }

        session.sendResponse(responseHttp);
    }

    private Response putProxy(final Request request,
                              final byte[] idArray,
                              final int node) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            byte[] body = request.getBody();
            body = Util.addTimestamp(body);
            final ByteBuffer value = Mapper.fromBytes(body);
            dao.upsert(key, value);
            responseHttp = Util.getResponseWithNoBody(Response.CREATED);
        } else {
            responseHttp = Util.routeRequest(request, node, nodeMapping);
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
    public void delete(final @Param("id") String idParam,
                       final Request request,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteInternal(idParam, request, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
    }

    /**
     * Delete replication.
     *
     * @param idParam - key.
     * @param session - session.
     */
    @Path("/v0/entity/rep")
    @RequestMethod(Request.METHOD_DELETE)
    public void deleteRep(final @Param("id") String idParam,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteRepInternal(idParam, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            }
        });
    }

    private void deleteRepInternal(final String idParam,
                                   final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
        }
        session.sendResponse(responseHttp);
    }

    private void deleteInternal(final String idParam,
                                final Request request,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final Request noRepRequest = GetHelper.getNoRepRequest(request, super.port);
            final Response responseHttpCurrent = deleteProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
                final Pair<Integer, Integer> ackFrom =
                        Util.getAckFrom(request, replicationDefaults, nodeMapping);
                final int from = ackFrom.getValue1();
                final List<Response> responses =
                        GetHelper.getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request, super.port);
                final Integer ack = ackFrom.getValue0();
                responseHttp = getEndResponsePutAndDelete(responses, ack, 202, nodeMapping);
            } else {
                responseHttp = responseHttpCurrent;
            }
        }
        session.sendResponse(responseHttp);
    }

    private Response deleteProxy(final Request request,
                                 final byte[] idArray,
                                 final int node) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
        } else {
            responseHttp = Util.routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
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
        final Response response = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response status() {
        return Util.getResponseWithNoBody(Response.OK);
    }

    @Override
    public synchronized void start() {
        super.start();
        dao.open();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
            executorService.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
