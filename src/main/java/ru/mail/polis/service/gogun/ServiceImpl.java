package ru.mail.polis.service.gogun;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import ru.mail.polis.dao.DAO;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServiceImpl extends HttpServer implements Service {
    @NotNull
    private final DAO dao;
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public ServiceImpl(final int port, final DAO dao) throws IOException {
        super(makeConfig(port));
        this.dao = dao;
    }

    @NotNull
    private static HttpServerConfig makeConfig(final int port) {
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;

        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};

        return config;
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        log.error("Can't understand request {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        super.handleDefault(request, session);
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    private ByteBuffer getBuffer(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    private byte[] getArray(final ByteBuffer buffer) {
        byte[] body;
        if (!buffer.hasRemaining()) {
            body = Response.EMPTY;
        } else {
            body = new byte[buffer.remaining()];
            buffer.get(body);
        }

        return body;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) @NotNull final String id)  {
        log.debug("GET request with id: {}", id);
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer buffer;

        try {
            buffer = dao.get(getBuffer(id.getBytes(UTF_8)));
        } catch (IOException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(getArray(buffer));
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response upsert(@Param(value = "id", required = true) @NotNull final String id, final Request request) {
        log.debug("PUT request with id: {}", id);
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        try {
            dao.upsert(getBuffer(id.getBytes(UTF_8)), getBuffer(request.getBody()));
        } catch (IOException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }


        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) @NotNull final String id)  {
        log.debug("DELETE request with id: {}", id);
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        try {
            dao.remove(getBuffer(id.getBytes(UTF_8)));
        } catch (IOException e) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
