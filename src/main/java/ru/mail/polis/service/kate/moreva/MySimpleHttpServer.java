package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Charsets;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * Simple implementation
 */

public class MySimpleHttpServer extends HttpServer implements Service {

    private final DAO dao;

    public MySimpleHttpServer(final int port, final DAO dao) throws IOException {
        super(getConfigPort(port));
        this.dao = dao;

    }

    private static HttpServerConfig getConfigPort(final int port) {
        if (port <= 1024 || port >= 65535) {
            throw new IllegalArgumentException("Invalid port");
        }
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    @Path("/v0/entity")
    public Response entity(@Param("id") final String id, final Request request) {
        if (id.isBlank()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return getEntity(key);
            case Request.METHOD_PUT:
                return putEntity(key, request);
            case Request.METHOD_DELETE:
                return deleteEntity(key);
            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    private Response getEntity(ByteBuffer key) {
        try {
            return new Response(Response.OK, dao.get(key).duplicate().array());
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response putEntity(ByteBuffer key, Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response deleteEntity(ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
