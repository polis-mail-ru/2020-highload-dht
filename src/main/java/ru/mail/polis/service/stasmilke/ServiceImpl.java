package ru.mail.polis.service.stasmilke;

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
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class ServiceImpl extends HttpServer implements Service {

    private final DAO dao;

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(prepareHttpServerConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig prepareHttpServerConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[] { acceptorConfig };
        return config;
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response getStatus() {
        return Response.ok(Response.OK);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * GET - get data by {@code ID}.
     * @param id - key
     * @return 200 OK, errors: 400, 404, 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getEntity(@Param(value = "id", required = true) final String id) {
        try {
            assertIsCorrectData(id);
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final ByteBuffer value = dao.get(key);
            return Response.ok(bufferToBytes(value));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IllegalArgumentException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
    }

    /**
     * PUT - upsert (create or replace) data by {@code ID}
     * @param id - key
     * @param request - value
     * @return 201 CREATED, errors: 400, 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putEntity(@Param(value = "id", required = true) final String id, final Request request) {
        try {
            assertIsCorrectData(id);
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            final ByteBuffer value = ByteBuffer.wrap(request.getBody());
            dao.upsert(key, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }catch (IllegalArgumentException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
    }

    /**
     * DELETE - remove data by {@code ID}.
     * @param id - key
     * @return 202 ACCEPTED, errors: 400, 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteEntity(@Param(value = "id", required = true) final String id) {
        try {
            assertIsCorrectData(id);
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (IllegalArgumentException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
    }

    private static void assertIsCorrectData(final String value) throws IllegalArgumentException {
        if (value.isEmpty() || value.isBlank()) {
            throw new IllegalArgumentException();
        }
    }

    private static byte[] bufferToBytes(@NotNull final ByteBuffer buffer) {
        final byte[] array = new byte[buffer.capacity()];
        buffer.get(array);
        return array;
    }
}
