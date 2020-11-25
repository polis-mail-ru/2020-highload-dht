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
import ru.mail.polis.session.StreamingSession;
import ru.mail.polis.util.Utility;

import java.io.IOException;

public class HttpEntityService extends HttpBasicService {
    private static final Logger logger = LoggerFactory.getLogger(HttpEntityService.class);
    final HttpEntityEntitiesHandler httpEntityEntitiesService;

    /**
     * Creates a new {@link HttpEntityService} with given port and {@link HttpEntityHandler}.
     *
     * @param port              listenable server's port
     * @param httpEntityEntitiesService entity request handler
     * @throws IOException rethrow from {@link HttpServer#HttpServer}
     */
    public HttpEntityService(final int port,
                             @NotNull final HttpEntityEntitiesHandler httpEntityEntitiesService) throws IOException {
        super(port);
        BasicConfigurator.configure();
        this.httpEntityEntitiesService = httpEntityEntitiesService;
    }

    /**
     * Entity request handler.
     *
     * @param id       request's param
     * @param replicas replica configuration
     * @param request  sent request
     * @param session  current session for network interaction
     * @throws IOException rethrow from {@link HttpSession#sendResponse} and {@link HttpEntityHandler#entity}
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (Utility.invalid(id)) {
            logger.error("Empty key");
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new IllegalArgumentException("Empty key");
        }

        httpEntityEntitiesService.entity(id, replicas, request, session);
    }

    /**
     * Entities request handler. Creates stream of records in
     * format(key '\n' value) that contains in range [start; end).
     *
     * @param start   start of range
     * @param end     end of range
     * @param session session for streaming
     * @throws IOException rethrow from {@link HttpSession#sendError} and {@link StreamingSession#stream}
     */
    @Path("/v0/entities")
    public void entities(@Param(value = "start", required = true) final String start,
                         @Param(value = "end") final String end,
                         final HttpSession session) throws IOException {
        if (Utility.invalid(start)) {
            session.sendError(Response.BAD_REQUEST, "Invalid start");
            return;
        }

        httpEntityEntitiesService.entities(start, end, (StreamingSession) session);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            httpEntityEntitiesService.close();
        } catch (IOException e) {
            logger.error("Error in closing entity service", e);
        }
    }
}
