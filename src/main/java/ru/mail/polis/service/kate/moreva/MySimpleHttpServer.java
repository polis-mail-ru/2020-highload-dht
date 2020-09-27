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
 * Simple Http Server Service implementation.
 * @author Kate Moreva
 */

public class MySimpleHttpServer extends HttpServer implements Service {

    private final DAO dao;

    /**
     * Http Server constructor.
     * */
    public MySimpleHttpServer(final int port, final DAO dao) throws IOException {
        super(getConfigPort(port));
        this.dao = dao;

    }

    private static HttpServerConfig getConfigPort(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    /**
     * Method to check whether the server is reachable or not.
     * If the server is available @return {@link Response} {@code 200}.
     * */
    @Path("/v0/status")
    public Response status() {
        return new Response(Response.OK, Response.EMPTY);
    }

    /**
     * Method for working with value in the DAO by the key.
     *
     * @return {@code 200, data} (data is found).
     *         {@code 404} (data is not found).
     *         {@code 201} (new data created).
     *         {@code 202} (data deleted).
     *         {@code 405} (unexpected method).
     *         {@code 500} (internal server error occurred).
     * */
    @Path("/v0/entity")
    public Response entity(@Param(value = "id", required = true) final String id, final Request request) {
        if (id.isBlank()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
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

    /**
     * Subsidiary method to get value.
     *
     * @return {@code 200, data} (data is found).
     *         {@code 404} (data is not found).
     *         {@code 500} (internal server error occurred).
     * */
    private Response getEntity(final ByteBuffer key) {
        try {
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] body = new byte[value.remaining()];
            value.get(body);
            return new Response(Response.OK, body);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Subsidiary method to put new value.
     *
     * @return {@code 201} (new data created).
     *         {@code 500} (internal server error occurred).
     * */
    private Response putEntity(final ByteBuffer key, final Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Subsidiary method to delete value by the key.
     *
     * @return {@code 202} (data deleted).
     *         {@code 500} (internal server error occurred).
     * */
    private Response deleteEntity(final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
