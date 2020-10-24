package ru.mail.polis.service.stasyanoi;

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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomServer extends HttpServer {
    private final Map<Integer, String> nodeMapping;
    private final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");
    private final int nodeCount;
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

    private void getRepInternal(String idParam, HttpSession session) throws IOException {
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
        Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            final ByteBuffer id = Mapper.fromBytes(idArray);
            final Request noRepRequest = getNoRepRequest(request);
            final Response responseHttpCurrent = getProxy(noRepRequest, node, id);
            tempNodeMapping.remove(node);
            if (request.getParameter("reps", "true").equals("true")) {
                Pair<Integer, Integer> ackFrom = Util.getAckFrom(request, replicationDefaults, nodeMapping);
                int from = ackFrom.getValue1();
                List<Response> responses = getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request);
                Integer ack = ackFrom.getValue0();
                responseHttp = getEndResponseGet(responses, ack);
            } else {
                responseHttp = responseHttpCurrent;
            }
        }


        session.sendResponse(responseHttp);
    }

    @NotNull
    private Response getEndResponseGet(List<Response> responses, Integer ack) {

        List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == 200)
                .collect(Collectors.toList());
        List<Response> emptyResponses = responses.stream()
                .filter(response -> response.getStatus() == 404)
                .collect(Collectors.toList());

        final Response responseHttp;

        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            if (goodResponses.size() > 0) {
                List<Pair<Long, Response>> resps = Stream.concat(emptyResponses.stream(), goodResponses.stream())
                        .filter(response -> response.getHeader("Time: ") != null)
                        .map(response -> new Pair<>(Long.parseLong(response.getHeader("Time: ")), response))
                        .collect(Collectors.toList());
                Map<Long, Response> map = new TreeMap<>();
                resps.forEach(pair -> map.put(pair.getValue0(), pair.getValue1()));
                ArrayList<Map.Entry<Long, Response>> entries = new ArrayList<>(map.entrySet());
                responseHttp = entries.get(entries.size() - 1).getValue();
            } else if (emptyResponses.size() >= ack) {
                responseHttp = Util.getResponseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = Util.getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }

    private List<Response> getResponsesInternal(Response responseHttpTemp, Map<Integer, String> tempNodeMapping,
                                                int from,
                                                Request request) {
        List<Response> responses = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>
                        (new HttpClient(new ConnectionString(nodeHost.getValue())), getNewRequest(request)))
                .map(clientRequest -> {
                    try {
                        Response invoke = clientRequest.getValue0().invoke(clientRequest.getValue1());
                        clientRequest.getValue0().close();
                        return invoke;
                    } catch (InterruptedException | PoolException | IOException | HttpException e) {
                        return Util.getResponseWithNoBody(Response.INTERNAL_ERROR);
                    }
                })
                .collect(Collectors.toList());
        responses.add(responseHttpTemp);

        return responses;
    }

    @NotNull
    private Request getNewRequest(Request request) {
        String path = request.getPath();
        String queryString = request.getQueryString();
        String newPath = path + "/rep?" + queryString;
        Request requestNew = getCloneRequest(request, newPath);
        requestNew.setBody(request.getBody());
        return requestNew;
    }

    @NotNull
    private Request getNoRepRequest(Request request) {
        String path = request.getPath();
        String queryString = request.getQueryString();
        String newPath;
        if (request.getHeader("reps") == null) {
            newPath = path + "?"+ queryString + "&reps=false";
        } else {
            newPath = path + "?"+ queryString;
        }
        Request noRepRequest = getCloneRequest(request, newPath);
        noRepRequest.setBody(request.getBody());
        return noRepRequest;
    }

    @NotNull
    private Request getCloneRequest(Request request, String newPath) {
        Request noRepRequest = new Request(request.getMethod(), newPath, true);
        Arrays.stream(request.getHeaders())
                .filter(Objects::nonNull)
                .filter(header -> !header.contains("Host: "))
                .forEach(noRepRequest::addHeader);
        noRepRequest.addHeader("Host: localhost:" + super.port);
        return noRepRequest;
    }

    private Response getProxy(final Request request,
                              final int node,
                              final ByteBuffer id) throws IOException {
        final Response responseHttp;
        if (node != nodeNum) {
            responseHttp = Util.routeRequest(request, node, nodeMapping, nodeNum);
        } else {
            responseHttp = getResponseIfIdNotNull(id);
        }
        return responseHttp;
    }

    @NotNull
    private Response getResponseIfIdNotNull(final ByteBuffer id) throws IOException {
        try {
            final ByteBuffer body = dao.get(id);
            byte[] bytes = Mapper.toBytes(body);
            Pair<byte[], byte[]> bodyTimestamp = Util.getTimestamp(bytes);
            byte[] newBody = bodyTimestamp.getValue0();
            byte[] time = bodyTimestamp.getValue1();
            Response ok = Response.ok(newBody);
            Util.addTimestampHeader(time, ok);
            return ok;
        } catch (NoSuchElementException e) {
            byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return Util.getResponseWithNoBody(Response.NOT_FOUND);
            } else {
                Response deletedResponse = Util.getResponseWithNoBody(Response.NOT_FOUND);
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

    private void putRepInternal(String idParam, Request request, HttpSession session) throws IOException {
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
        Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            Request noRepRequest = getNoRepRequest(request);
            final Response responseHttpCurrent = putProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            if (request.getParameter("reps", "true").equals("true")) {
                Pair<Integer, Integer> ackFrom = Util.getAckFrom(request, replicationDefaults, nodeMapping);
                int from = ackFrom.getValue1();
                List<Response> responses = getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request);
                Integer ack = ackFrom.getValue0();
                responseHttp = getEndResponsePutAndDelete(responses, ack, 201);
            } else {
                responseHttp = responseHttpCurrent;
            }
        }

        session.sendResponse(responseHttp);
    }

    @NotNull
    private Response getEndResponsePutAndDelete(List<Response> responses, Integer ack, int status) {
        final Response responseHttp;
        List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == status)
                .collect(Collectors.toList());
        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            if (goodResponses.size() >= ack) {
                if (status == 202) {
                    responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
                } else {
                    responseHttp = Util.getResponseWithNoBody(Response.CREATED);
                }
            } else {
                responseHttp = Util.getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }

    private Response putProxy(final Request request,
                              final byte[] idArray,
                              final int node) throws IOException {
        final Response responseHttp;
        if (node != nodeNum) {
            responseHttp = Util.routeRequest(request, node, nodeMapping, nodeNum);
        } else {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            byte[] body = request.getBody();
            body = Util.addTimestamp(body);
            final ByteBuffer value = Mapper.fromBytes(body);
            dao.upsert(key, value);
            responseHttp = Util.getResponseWithNoBody(Response.CREATED);
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

    private void deleteRepInternal(String idParam, HttpSession session) throws IOException {
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
        Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeMapping);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = Util.getNode(idArray, nodeCount);
            Request noRepRequest = getNoRepRequest(request);
            Response responseHttpCurrent = deleteProxy(noRepRequest, idArray, node);
            tempNodeMapping.remove(node);
            if (request.getParameter("reps", "true").equals("true")) {
                Pair<Integer, Integer> ackFrom = Util.getAckFrom(request, replicationDefaults, nodeMapping);
                int from = ackFrom.getValue1();
                List<Response> responses = getResponsesInternal(responseHttpCurrent, tempNodeMapping, from - 1, request);
                Integer ack = ackFrom.getValue0();
                responseHttp = getEndResponsePutAndDelete(responses, ack, 202);
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
        if (node != nodeNum) {
            responseHttp = Util.routeRequest(request, node, nodeMapping, nodeNum);
        } else {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
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
