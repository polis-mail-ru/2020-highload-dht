package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Utility;

import java.io.IOException;

public class HttpService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);
    final HttpEntityHandler httpEntityService;

    /**
     * Creates a new {@link HttpService} with given port and {@link HttpEntityHandler}.
     * @param port listenable server's port
     * @param httpEntityService entity request handler
     * @throws IOException rethrow from {@link HttpServer#HttpServer}
     */
    public HttpService(final int port, @NotNull final HttpEntityHandler httpEntityService) throws IOException {
        super(Utility.configFrom(port));
        BasicConfigurator.configure();
        this.httpEntityService = httpEntityService;
    }

    /**
     * Entity request handler.
     * @param id request's param
     * @param replicas replica configuration
     * @param request sent request
     * @param session current session for network interaction
     * @throws IOException rethrow from {@link HttpSession#sendResponse} and {@link HttpEntityHandler#entity}
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (!Utility.validateId(id)) {
            logger.error("Empty key");
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new IllegalArgumentException("Empty key");
        }

        httpEntityService.entity(id, replicas, request, session);
    }

    /**
     * Handling status request.
     *
     * @param session current Session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
        session.sendResponse(Response.ok("OK"));
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            httpEntityService.close();
        } catch (IOException e) {
            logger.error("Error in closing entity service", e);
        }
    }
}
