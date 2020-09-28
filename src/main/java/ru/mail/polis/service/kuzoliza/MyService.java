package ru.mail.polis.service.kuzoliza;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class MyService extends HttpServer implements Service {

    private final DAO dao;
    private static final Logger log = LoggerFactory.getLogger(MyService.class.getName());

    MyService(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response status() {
        return Response.ok("OK");
    }

    @Path("/v0/entity")
    public Response entity(final @Param(value = "id", required = true) String id, final Request request) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            return response(key, request);
        } catch (IOException e) {
            log.error("Can't process response");
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response response(final ByteBuffer key, final Request request) throws IOException {
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                try {
                    final ByteBuffer value = dao.get(key);
                    return Response.ok(toByteArray(value));
                } catch (NoSuchElementException e) {
                    log.error("Can't find resource");
                    return new Response(Response.NOT_FOUND, Response.EMPTY);
                }

            case Request.METHOD_PUT:
                dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                return new Response(Response.CREATED, Response.EMPTY);

            case Request.METHOD_DELETE:
                dao.remove(key);
                return new Response(Response.ACCEPTED, Response.EMPTY);

            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    private static byte[] toByteArray(final ByteBuffer value) {
        if (!value.hasRemaining()) {
            return Response.EMPTY;
        }

        final byte[] response = new byte[value.remaining()];
        value.get(response);
        return response;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

}
