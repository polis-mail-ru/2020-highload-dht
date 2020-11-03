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
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class CustomServer extends HttpServer {
    private final Map<Integer, String> nodeMapping;
    private final Map<String, HttpClient> httpClientMap;
    private final int nodeCount;
    private int nodeNum;
    private final DAO dao;
    private final CustomExecutor executorService = CustomExecutor.getExecutor();

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
        urls.sort(String::compareTo);

        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();
        final Map<String, HttpClient> clients = new HashMap<>();

        for (int i = 0; i < urls.size(); i++) {
            nodeMappingTemp.put(i, urls.get(i));
            clients.put(urls.get(i), new HttpClient(new ConnectionString(urls.get(i))));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
        }
        this.httpClientMap = clients;
        this.nodeMapping = nodeMappingTemp;
        this.dao = dao;
    }

    private int getNode(final byte[] idArray) {
        final int abs = Math.abs(Arrays.hashCode(idArray));
        final int hash = abs < 0 ? -abs : abs;
        return hash % nodeCount;
    }

    private Response routeRequest(final Request request, final int node) throws IOException {
        final HttpClient httpClient = httpClientMap.get(nodeMapping.get(node));
        try {
            return httpClient.invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            return Util.getResponseWithNoBody(Response.INTERNAL_ERROR);
        }
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
        try {
            executorService.execute(() -> {
                try {
                    getInternal(idParam, session, request);
                } catch (IOException e) {
                    Util.sendErrorInternal(session, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Util.send503Error(session);
        }
    }

    private void getInternal(final String idParam,
                             final HttpSession session,
                             final Request request) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
            final int node = getNode(idArray);
            final ByteBuffer id = Mapper.fromBytes(idArray);
            responseHttp = getThisOrProxy(request, node, id);
        }
        session.sendResponse(responseHttp);
    }

    private Response getThisOrProxy(final Request request,
                                    final int node,
                                    final ByteBuffer id) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            responseHttp = getResponseIfIdNotNull(id);
        } else {
            responseHttp = routeRequest(request, node);
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
            return Util.getResponseWithNoBody(Response.NOT_FOUND);
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
        try {
            executorService.execute(() -> {
                try {
                    putInternal(idParam, request, session);
                } catch (IOException e) {
                    Util.sendErrorInternal(session, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Util.send503Error(session);
        }
    }

    private void putInternal(final String idParam,
                             final Request request,
                             final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
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
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            final ByteBuffer value = Mapper.fromBytes(request.getBody());
            dao.upsert(key, value);
            responseHttp = Util.getResponseWithNoBody(Response.CREATED);
        } else {
            responseHttp = routeRequest(request, node);
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
        try {
            executorService.execute(() -> {
                try {
                    deleteInternal(idParam, request, session);
                } catch (IOException e) {
                    Util.sendErrorInternal(session, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Util.send503Error(session);
        }
    }

    private void deleteInternal(final String idParam,
                                final Request request,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
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
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
        } else {
            responseHttp = routeRequest(request, node);
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
            executorService.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
