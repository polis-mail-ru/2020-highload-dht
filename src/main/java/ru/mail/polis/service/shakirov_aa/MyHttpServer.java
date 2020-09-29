package ru.mail.polis.service.shakirov_aa;

import com.google.common.base.Charsets;
import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class MyHttpServer extends HttpServer implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyHttpServer.class);

    @NotNull
    private final DAO dao;

    public MyHttpServer(final int port, @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig getConfig(final int port) {
        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("Port out of range");
        }

        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        acceptorConfig.deferAccept = true;

        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = 4;
        return config;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    @Path("/v0/entity")
    public Response entity(@NotNull final Request request, @Param("id") final String id) {
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    ByteBuffer value = dao.get(key);
                    ByteBuffer duplicate = value.duplicate();
                    byte[] body = new byte[duplicate.remaining()];
                    duplicate.get(body);
                    return new Response(Response.OK, body);
                case Request.METHOD_PUT:
                    dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                    return new Response(Response.CREATED, Response.EMPTY);
                case Request.METHOD_DELETE:
                    dao.remove(key);
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, "Key not found".getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        LOGGER.error("Unknown request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
