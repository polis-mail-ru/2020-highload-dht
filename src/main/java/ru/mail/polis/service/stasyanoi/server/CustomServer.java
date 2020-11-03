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
import ru.mail.polis.service.stasyanoi.Util;
import ru.mail.polis.service.stasyanoi.server.internal.FrameServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionException;

import static ru.mail.polis.service.stasyanoi.Util.routeRequest;

public class CustomServer extends FrameServer {

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
            final Pair<Map<Integer, String>, Map<Integer, String>> mappings =
                    new Pair<>(tempNodeMapping, nodeMapping);
            responseHttp = getPutReplicaResponse(request, mappings,
                    responseHttpCurrent, super.port);
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
}
