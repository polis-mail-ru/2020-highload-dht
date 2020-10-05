package ru.mail.polis.service.igor_manikhin;

import one.nio.http.Path;
import one.nio.http.Param;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;

import org.jetbrains.annotations.NotNull;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class ServiceImpl extends HttpServer implements Service {
    private final DAO dao;

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(getServerConfig(port));
        this.dao = dao;
    }


    @Path("/v0/status")
    public Response status() {
        return Response.ok("Ok!");
    }

    @Path("/v0/entity")
    public Response entity(@Param(value = "id", required = true) final String id,
                           @NotNull final Request request
    ) {
        try {
            if (id.isEmpty()) {
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }

            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    return get(key);
                }
                case Request.METHOD_PUT: {
                   return put(key, request);
                }
                case Request.METHOD_DELETE: {
                    return delete(key);
                }
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (Exception ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response get(final ByteBuffer key) {
        try {
            final ByteBuffer value = dao.get(key);
            final ByteBuffer duplicate = value.duplicate();
            final byte[] body = new byte[duplicate.remaining()];
            duplicate.get(body);
            return new Response(Response.OK, body);
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response put(final ByteBuffer key, final Request request) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(request.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response delete(final ByteBuffer key) throws IOException {
        dao.remove(key);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    private static HttpServerConfig getServerConfig(final int port) throws IllegalArgumentException{
        if (port <= 1024 || 65536 <= port) {
            throw new IllegalArgumentException("invalid port");
        }

        AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }

}
