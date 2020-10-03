package ru.mail.polis.service.art241111;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.art241111.handlers.EntityHandler;
import ru.mail.polis.service.art241111.handlers.StatusHandler;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple {@link Service} implementation.
 */
public class MyHttpServer extends HttpServer implements Service {
    private final DAO dao;

    public MyHttpServer(final int port,
                        @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    @Override
    public void handleDefault(final Request request,
                              final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        super.handleDefault(request, session);
    }

    /**
     * We begin to observe the data that comes along the way "/v0/entity".
     */
    @Path("/v0/entity")
    public Response entity(
            @Param(value = "id", required = true) final String id,
            final Request request) {

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
        final EntityHandler handlers = new EntityHandler(key, dao);

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return handlers.handlerGetRequest();
            case Request.METHOD_PUT:
                return handlers.handlerPutRequest(request);
            case Request.METHOD_DELETE:
                return handlers.handlerDeleteRequest();
            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    /**
     * We begin to observe the data that comes along the way "/v0/status".
     */
    @Path("/v0/status")
    public Response status() {
        return new StatusHandler().handlerStatusRequest();
    }

    private static HttpServerConfig getConfig(final int port) {
        final HttpServerConfig config = new HttpServerConfig();
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }
}
