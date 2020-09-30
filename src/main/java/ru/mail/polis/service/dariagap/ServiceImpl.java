package ru.mail.polis.service.dariagap;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.*;

public class ServiceImpl extends HttpServer implements Service {

    @NotNull
    final private DAO dao;

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(formConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig formConfig(final int port) {
        final HttpServerConfig conf = new HttpServerConfig();
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        conf.acceptors = new AcceptorConfig[]{ac};
        return conf;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Get data by id.
     *
     * @param id key of entity
     * @return response OK with value or status BAD_REQUEST, INTERNAL_ERROR, NOT_FOUND
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id) {
        if(id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            ByteBuffer value = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.OK, Util.byteBufferToBytes(value));
        }
        catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        catch (NoSuchElementException ex)
        {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    /**
     * Set data by id.
     *
     * @param id key of entity
     * @param request request with the entity value in body
     * @return status CREATED or status BAD_REQUEST, INTERNAL_ERROR
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param("id") final String id,
                        @Param("request") final Request request) {
        if(id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            dao.upsert(ByteBuffer.wrap(id.getBytes(UTF_8)),
                    ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        }
        catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete data by id.
     *
     * @param id key of entity
     * @return status ACCEPTED or status BAD_REQUEST, INTERNAL_ERROR
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param("id") final String id) {
        if(id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            dao.remove(ByteBuffer.wrap(id.getBytes(UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }
        catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }
}
