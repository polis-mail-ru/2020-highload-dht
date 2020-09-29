package ru.mail.polis.service.kirkungurov;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class HttpService extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpService.class);

    @NotNull
    private static HttpServerConfig createConfig(final int port){
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[] {acceptorConfig};
        return  httpServerConfig;
    }

    public HttpService(final int port, @NotNull DAO dao) throws IOException {
        super(createConfig(port));
        this.dao = dao;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        LOGGER.error("Not implemented");
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Get status of server
     * @return 200 - OK
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    /**
     * Requst to get Entity by id
     * @param id - Entity id
     * @return 200 - OK
     *         400 - Empty id in param
     *         404 - No such element in dao
     *         500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id) {
        try {
            if (id.length() == 0){
                LOGGER.error("GET: get empty id in param");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            return new Response(Response.OK, convertFromBB(dao.get(convertToBB(id))));
        } catch (NoSuchElementException exc) {
            LOGGER.error("GET: can't found element with id: {}", id);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error("GET: can't get element with id: {}, errorMsg: {}", id, exc.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete entity from dao by id
     * @param id - Entity id
     * @return 202 - Delete entity
     *         400 - Empty id in param
     *         500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id) {
        try {
            if (id.length() == 0){
                LOGGER.error("DELETE: get empty id in param");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.remove(convertToBB(id));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error("DELETE: can't get element with id: {}, errorMsg: {}", id, exc.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete entity from dao by id
     * @param id - Entity id
     * @return 201 - Created entity
     *         400 - Empty id in param
     *         500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id, @NotNull Request request) {
        try {
            if (id.length() == 0){
                LOGGER.error("PUT: get empty id in param");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(convertToBB(id), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException exc) {
            LOGGER.error("PUT: can't get element with id: {}, request: {}, errorMsg: {}", id, request.getBody(), exc.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private static ByteBuffer convertToBB(@NotNull final String param) {
        return ByteBuffer.wrap(param.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] convertFromBB(@NotNull final ByteBuffer bb) {
        if (!bb.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] response = new byte[bb.remaining()];
        bb.get(response);
        return response;
    }
}
