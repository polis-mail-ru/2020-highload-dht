package ru.mail.polis.service.jhoysbou;

import one.nio.http.*;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class LsmServer extends HttpServer implements Service {
    private final DAO dao;

    public LsmServer(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getValue(@Param(value = "id", required = true) final String id) {
        if (id.equals("")) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        ByteBuffer value = null;
        try {
            value = dao.get(idBuffer);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        final byte[] array = new byte[value.remaining()];
        value.get(array);
        return new Response(Response.OK, array);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putValue(@Param(value = "id", required = true) final String id,
                             final Request request) {
        if (id.equals("")) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final ByteBuffer valueBuffer = ByteBuffer.wrap(request.getBody());
        try {
            dao.upsert(idBuffer, valueBuffer);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteValue(@Param(value = "id", required = true) final String id) throws IOException {
        if (id.equals("")) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        dao.remove(idBuffer);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }
}
