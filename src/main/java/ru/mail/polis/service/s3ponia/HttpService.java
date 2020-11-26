package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.MapIterator;
import ru.mail.polis.util.Utility;

import java.io.IOException;
import java.util.Iterator;

public class HttpService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);
    final HttpEntityHandler httpEntityService;
    final EntitiesService entitiesService;

    /**
     * Creates a new {@link HttpService} with given port and {@link HttpEntityHandler}.
     *
     * @param port              listenable server's port
     * @param httpEntityService entity request handler
     * @param entitiesService   wrapper around DAO for providing range iterator
     * @throws IOException rethrow from {@link HttpServer#HttpServer}
     */
    public HttpService(final int port,
                       @NotNull final HttpEntityHandler httpEntityService,
                       @NotNull final EntitiesService entitiesService) throws IOException {
        super(Utility.configFrom(port));
        this.entitiesService = entitiesService;
        BasicConfigurator.configure();
        this.httpEntityService = httpEntityService;
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
        if (!Utility.validateId(id)) {
            logger.error("Empty key");
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new IllegalArgumentException("Empty key");
        }

        httpEntityService.entity(id, replicas, request, session);
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
        if (!Utility.validateId(start)) {
            session.sendError(Response.BAD_REQUEST, "Invalid start");
            return;
        }

        final Iterator<Record> recordIterator;

        if (end == null) {
            recordIterator = entitiesService.from(
                    Utility.byteBufferFromString(start)
            );
        } else {
            recordIterator = entitiesService.range(
                    Utility.byteBufferFromString(start),
                    Utility.byteBufferFromString(end)
            );
        }

        final Iterator<StreamingValue> streamIterator = new MapIterator<>(
                recordIterator,
                StreamingRecordValue::new
        );

        ((StreamingSession) session).stream(streamIterator);
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
    public HttpSession createSession(@NotNull final Socket socket) {
        return new StreamingSession(socket, this);
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
