package ru.mail.polis.service.nik27090;

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

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final DAO dao;

    public ServiceImpl(final int port, final @NotNull DAO dao) throws IOException {
        super(createConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Get data by key
     *
     * @param id - key for storage
     * @return - code 200 and data,
     *           code 400 if id is empty,
     *           code 404 if data not found,
     *           code 500 if internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getEntity(final @Param(value = "id", required = true) String id) {
        log.debug("GET request: id = {}", id);

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer value;

        try {
            value = dao.get(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8))).duplicate();
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("Internal error with id = {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        final byte[] response = new byte[value.remaining()];
        value.get(response);

        return Response.ok(response);
    }

    /**
     * Create/overwrite data by key
     *
     * @param id - key for storage
     * @param request - body request
     * @return - 200 if value has been created/overwritten,
     *           400 if id is empty,
     *           500 if internal error
     *
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putEntity(@Param(value = "id", required = true) final String id, final Request request) {
        log.debug("PUT request: id = {}, value length = {}", id, request.getBody().length);

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());

        try {
            dao.upsert(key, value);
        } catch (IOException e) {
            log.error("Internal error with id = {}, value length = {}", id, request.getBody().length, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * Delete data by key
     *
     * @param id - key for storage
     * @return - 200 if value was remote
     *           500 if internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteEntity(@Param(value = "id", required = true) final String id) {
        log.debug("DELETE request: id = {}", id);

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        try {
            dao.remove(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("Internal error with id = {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        log.debug("Can't understand request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
