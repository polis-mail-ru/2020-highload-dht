package ru.mail.polis.service.stakenschneider;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.NoSuchElementLite;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServiceImplementation extends HttpServer implements Service {
    private final DAO dao;

    public ServiceImplementation(final int port,
                     @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    @Path("/v0/status")
    public Response status() { return Response.ok("OK");}

    @Path("/v0/entity")
    public Response entity(
            @Param("id") final String id,
            @NotNull final Request request
    ) {
        try {
            if (id == null || id.isEmpty()) {
                return new Response(Response.BAD_REQUEST, "Id must be not null".getBytes(StandardCharsets.UTF_8));
            }
            final var key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    try {
                        ByteBuffer value = dao.get(key);
                        ByteBuffer duplicate = value.duplicate();
                        byte[] body = new byte[duplicate.remaining()];
                        duplicate.get(body);
                        return new Response(Response.OK, body);
                    } catch (NoSuchElementLite ex) {
                        return new Response(Response.NOT_FOUND, Response.EMPTY);
                    }
                }

                case Request.METHOD_PUT: {
                    dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                    return new Response(Response.CREATED, Response.EMPTY);
                }
                case Request.METHOD_DELETE: {
                    dao.remove(key);
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                }
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (Exception ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final var response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    private static HttpServerConfig getConfig(final int port) {
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
