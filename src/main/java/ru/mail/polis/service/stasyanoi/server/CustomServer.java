package ru.mail.polis.service.stasyanoi.server;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.javatuples.Pair;
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
        executorService.execute(() -> {
            try {
                getInternal(idParam, session, request);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            } catch (RejectedExecutionException e) {
                Util.send503Error(session);
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
    public void getRep(final @Param("id") String idParam, final HttpSession session) {
        executorService.execute(() -> {
            try {
                getRepInternal(idParam, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            } catch (RejectedExecutionException e) {
                Util.send503Error(session);
            }
        });
    }

    private void getRepInternal(final String idParam, final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer id = Util.getKey(idParam);
            responseHttp = getResponseIfIdNotNull(id, dao);
        }
        session.sendResponse(responseHttp);
    }

    private void getInternal(final String idParam, final HttpSession session,
                             final Request request) throws IOException {
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
        session.sendResponse(responseHttp);
    }

    private Response getProxy(final Request request, final int node, final ByteBuffer id) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            responseHttp = getResponseIfIdNotNull(id, dao);
        } else {
            responseHttp = Util.routeRequest(request, node, nodeMapping);
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
        executorService.execute(() -> {
            try {
                putInternal(idParam, request, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            } catch (RejectedExecutionException e) {
                Util.send503Error(session);
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
    public void putRep(final @Param("id") String idParam, final Request request, final HttpSession session) {
        executorService.execute(() -> {
            try {
                putRepInternal(idParam, request, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            } catch (RejectedExecutionException e) {
                Util.send503Error(session);
            }
        });
    }

    private void putRepInternal(final String idParam, final Request request,
                                final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer key = Util.getKey(idParam);
            final ByteBuffer value = Util.getByteBufferValue(request);
            dao.upsert(key, value);
            responseHttp = Util.responseWithNoBody(Response.CREATED);
        }
        session.sendResponse(responseHttp);
    }

    private void putInternal(final String idParam, final Request request,
                             final HttpSession session) throws IOException {

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
        session.sendResponse(responseHttp);
    }

    private Response putProxy(final Request request, final byte[] idArray, final int node) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            final ByteBuffer value = Util.getByteBufferValue(request);
            dao.upsert(key, value);
            responseHttp = Util.responseWithNoBody(Response.CREATED);
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
    public void delete(final @Param("id") String idParam, final Request request, final HttpSession session) {
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
    public void deleteRep(final @Param("id") String idParam, final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteRepInternal(idParam, session);
            } catch (IOException e) {
                Util.sendErrorInternal(session, e);
            } catch (RejectedExecutionException e) {
                Util.send503Error(session);
            }
        });
    }

    private void deleteRepInternal(final String idParam, final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer key = Util.getKey(idParam);
            dao.remove(key);
            responseHttp = Util.responseWithNoBody(Response.ACCEPTED);
        }
        session.sendResponse(responseHttp);
    }

    private void deleteInternal(final String idParam, final Request request,
                                final HttpSession session) throws IOException {
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
        session.sendResponse(responseHttp);
    }

    private Response deleteProxy(final Request request, final byte[] idArray, final int node) throws IOException {
        final Response responseHttp;
        if (node == nodeNum) {
            final ByteBuffer key = Mapper.fromBytes(idArray);
            dao.remove(key);
            responseHttp = Util.responseWithNoBody(Response.ACCEPTED);
        } else {
            responseHttp = Util.routeRequest(request, node, nodeMapping);
        }
        return responseHttp;
    }
}
