package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class CustomServer extends HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(CustomServer.class);

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
     * @param dao      - DAO to use.
     * @param config   - config for server.
     * @param topology - topology of services.
     * @throws IOException - if an IO exception occurs.
     */
    public CustomServer(final DAO dao,
                        final HttpServerConfig config,
                        final Set<String> topology) throws IOException {
        super(config);
        this.nodeCount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        urls.sort(String::compareTo);

        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();

        for (int i = 0; i < urls.size(); i++) {
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
            nodeMappingTemp.put(i, urls.get(i));
        }

        this.nodeMapping = nodeMappingTemp.entrySet().stream()
                .filter(integerStringEntry -> !integerStringEntry.getValue()
                        .contains(String.valueOf(super.port)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.dao = dao;
    }

    private int getNode(final byte[] idArray) {
        final int hash = Math.abs(Arrays.hashCode(idArray));
        return hash % nodeCount;
    }

    private Response routeRequest(final Request request, final int node) throws IOException {
        try {
            final ConnectionString connectionString = new ConnectionString(nodeMapping.get(node));
            final HttpClient httpClient = new HttpClient(connectionString);
            Response invoke = httpClient.invoke(request);
            httpClient.close();
            return invoke;
        } catch (InterruptedException | PoolException | HttpException e) {
            return getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
        }
    }

    private Pair<Integer, Integer> getAckFrom(Request request) {
        int ack;
        int from;
        String replicas = request.getParameter("replicas");
        if (replicas == null) {
            Optional<String[]> ackFrom = replicationDefaults.stream()
                    .map(replic -> replic.split("/"))
                    .filter(strings -> Integer.parseInt(strings[1]) == (nodeMapping.size() + 1))
                    .findFirst();
            ack = Integer.parseInt(ackFrom.get()[0]);
            from = Integer.parseInt(ackFrom.get()[1]);
        } else {
            replicas = replicas.substring(1);
            ack = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 0));
            from = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 1));
        }

        return new Pair<>(ack, from);
    }

    @NotNull
    private Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    private void sendErrorInternal(final HttpSession session,
                                   final IOException e) {
        try {
            logger.error(e.getMessage(), e);
            session.sendError("500", e.getMessage());
        } catch (IOException exception) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Get a record by key.
     *
     * @param idParam - key.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public void get(final @Param("id") String idParam,
                    final HttpSession session,
                    final Request request) {
        executorService.execute(() -> {
            try {
                getInternal(idParam, session, request);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    @Path("/v0/entity/rep")
    @RequestMethod(METHOD_GET)
    public void getRep(final @Param("id") String idParam,
                    final HttpSession session,
                    final Request request) {
        executorService.execute(() -> {
            try {
                getRepInternal(idParam, session, request);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void getRepInternal(String idParam, HttpSession session, Request request) throws IOException {
        final Response responseHttp;
        //check id param
        System.out.println(request);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            //get id as aligned byte buffer
            final ByteBuffer id = Mapper.fromBytes(idArray);
            //get the response from db
            responseHttp = getResponseIfIdNotNull(id);
        }

        session.sendResponse(responseHttp);
    }

    private void getInternal(final String idParam,
                             final HttpSession session,
                             final Request request) throws IOException {
        final Response responseHttp;
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = getNode(idArray);
            //get id as aligned byte buffer
            final ByteBuffer id = Mapper.fromBytes(idArray);
            //get the response from db
            responseHttp = getProxy(request, node, id);
        }

        session.sendResponse(responseHttp);
    }

    private Response getProxy(final Request request,
                              final int node,
                              final ByteBuffer id) throws IOException {
        final Response responseHttpTemp;
        String temp = null;
        if (nodeMapping.containsKey(node)) {
            responseHttpTemp = routeRequest(request, node);
            if (responseHttpTemp.getStatus() == 500){
                temp = nodeMapping.remove(node);
                nodeMapping.put(nodeNum, "http://localhost:" + super.port);
            }
        } else
            {
            //replicate here
            responseHttpTemp = getResponseIfIdNotNull(id);

        }

        Response responseGet = getResponseGet(request, responseHttpTemp, 200);

        if (temp != null) {
            nodeMapping.remove(nodeNum);
            nodeMapping.put(node, temp);
        }

        return responseGet;
    }

    private Response getResponseGet(Request request, Response responseHttpTemp, int statusCode) {
        final Response responseHttp;
        Pair<Integer, Integer> ackFrom = getAckFrom(request);
        String path = request.getPath();
        String queryString = request.getQueryString();

        String newPath = path + "/rep?" + queryString;

        Request requestNew = new Request(request.getMethod(), newPath, true);
        requestNew.setBody(request.getBody());


        Integer from = ackFrom.getValue1() - 1;
        List<Response> responses = nodeMapping.entrySet().stream().limit(from)
                .map(Map.Entry::getValue)
                .map(url -> new HttpClient(new ConnectionString(url)))
                .map(client -> {
                    try {
                        Response invoke = client.invoke(requestNew);
                        client.close();
                        return invoke;
                    } catch (InterruptedException | IOException | PoolException | HttpException e) {
                        return getResponseWithNoBody(Response.INTERNAL_ERROR);
                    }
                })
                .collect(Collectors.toList());
        responses.add(responseHttpTemp);

        List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == statusCode
                        && response.getBody().length != 0)
                .collect(Collectors.toList());
        Integer ack = ackFrom.getValue0();
        ++from;
        if (ack > from) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            if (goodResponses.size() >= ack) {
                responseHttp = Response.ok(goodResponses.get(0).getBody());
            } else if (goodResponses.size() > 0) {
                responseHttp = getResponseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }


    @NotNull
    private Response getResponseIfIdNotNull(final ByteBuffer id) throws IOException {
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            return Response.ok(bytes);
        } catch (NoSuchElementException e) {
            //if not found then 404
            return getResponseWithNoBody(Response.NOT_FOUND);
        }
    }

    /**
     * Create or update a record.
     *
     * @param idParam - key of the record.
     * @param request - request with the body.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public void put(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putInternal(idParam, request, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    @Path("/v0/entity/rep")
    @RequestMethod(METHOD_PUT)
    public void putRep(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putRepInternal(idParam, request, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void putRepInternal(String idParam, Request request, HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final ByteBuffer key = Mapper.fromBytes(idArray);
            final ByteBuffer value = Mapper.fromBytes(request.getBody());
            dao.upsert(key, value);
            responseHttp = getResponseWithNoBody(Response.CREATED);
        }
        session.sendResponse(responseHttp);

    }

    private void putInternal(final String idParam,
                             final Request request,
                             final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = getPutResponse(idParam, request);
        }
        session.sendResponse(responseHttp);
    }

    private Response getPutResponse(final String idParam, final Request request) throws IOException {
        final Response responseHttp;
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int node = getNode(idArray);
        responseHttp = putProxy(request, idArray, node);
        return responseHttp;
    }

    private Response putProxy(final Request request,
                              final byte[] idArray,
                              final int node) throws IOException {
        final Response responseHttpTemp;
        String temp = null;
        if (nodeMapping.containsKey(node)) {
            responseHttpTemp = routeRequest(request, node);
            if (responseHttpTemp.getStatus() == 500){
                temp = nodeMapping.remove(node);
                nodeMapping.put(nodeNum, "http://localhost:" + super.port);
            }
        } else {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            final ByteBuffer value = Mapper.fromBytes(request.getBody());
            dao.upsert(key, value);
            responseHttpTemp = getResponseWithNoBody(Response.CREATED);
        }

        Response response = getResponse(request, responseHttpTemp, 201, Response.CREATED);

        if (temp != null) {
            nodeMapping.remove(nodeNum);
            nodeMapping.put(node, temp);
        }

        return response;
    }

    /**
     * Delete a record.
     *
     * @param idParam - key of the record to delete.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public void delete(final @Param("id") String idParam,
                       final Request request,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteInternal(idParam, request, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    @Path("/v0/entity/rep")
    @RequestMethod(METHOD_DELETE)
    public void deleteRep(final @Param("id") String idParam,
                       final Request request,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteRepInternal(idParam, request, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void deleteRepInternal(String idParam, Request request, HttpSession session) throws IOException {
        final Response responseHttp;
        System.out.println(request);
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp= getResponseWithNoBody(Response.ACCEPTED);

        }
        session.sendResponse(responseHttp);
    }

    private void deleteInternal(final String idParam,
                                final Request request,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = getNode(idArray);
            responseHttp = deleteProxy(request, idArray, node);
        }
        session.sendResponse(responseHttp);
    }

    private Response deleteProxy(final Request request,
                                 final byte[] idArray,
                                 final int node) throws IOException {
        final Response responseHttpTemp;
        String temp = null;
        if (nodeMapping.containsKey(node)) {
            responseHttpTemp = routeRequest(request, node);
            if (responseHttpTemp.getStatus() == 500){
                temp = nodeMapping.remove(node);
                nodeMapping.put(nodeNum, "http://localhost:" + super.port);
            }
        } else {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttpTemp = getResponseWithNoBody(Response.ACCEPTED);
        }

        Response response = getResponse(request, responseHttpTemp, 202, Response.ACCEPTED);

        if (temp != null) {
            nodeMapping.remove(nodeNum);
            nodeMapping.put(node, temp);
        }

        return response;
    }

    private Response getResponse(Request request, Response responseHttpTemp, int statusCode, String goodStatus) {
        final Response responseHttp;
        Pair<Integer, Integer> ackFrom = getAckFrom(request);
        String path = request.getPath();
        String queryString = request.getQueryString();
        String newPath = path + "/rep?" + queryString;

        Request requestNew = new Request(request.getMethod(), newPath, true);
        requestNew.setBody(request.getBody());
        Arrays.stream(request.getHeaders()).filter(Objects::nonNull)
                .filter(header -> !header.contains("Host: "))
                .forEach(requestNew::addHeader);


        int from = ackFrom.getValue1() - 1;
        Set<Map.Entry<Integer, String>> entries = nodeMapping.entrySet();

        List<Map.Entry<Integer, String>> collect = entries.stream().limit(from).collect(Collectors.toList());

        List<Response> responses = collect.stream()
                .map(Map.Entry::getValue)
                .map(url -> new Pair<>(new HttpClient(new ConnectionString(url)),url))
                .map(pair -> {
                    try {
                        requestNew.addHeader("Host: " + pair.getValue1()
                                .substring(pair.getValue1().lastIndexOf("/") + 1));
                        HttpClient client = pair.getValue0();
                        Response invoke = client.invoke(requestNew);
                        client.close();
                        return invoke;
                    } catch (InterruptedException | IOException | PoolException | HttpException e) {
                        return getResponseWithNoBody(Response.INTERNAL_ERROR);
                    }
                })
                .collect(Collectors.toList());

        responses.add(responseHttpTemp);

        List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == statusCode)
                .collect(Collectors.toList());
        Integer ack = ackFrom.getValue0();
        ++from;
        if (ack > from) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            if (goodResponses.size() >= ack) {
                responseHttp = getResponseWithNoBody(goodStatus);
            } else {
                responseHttp = getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
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
        final Response response = getResponseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        return getResponseWithNoBody(Response.OK);
    }

    @Override
    public synchronized void start() {
        super.start();
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
