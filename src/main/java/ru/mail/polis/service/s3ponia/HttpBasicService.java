package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.Service;
import ru.mail.polis.util.Utility;

import java.io.IOException;

public class HttpBasicService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(HttpBasicService.class);

    /**
     * Creates a new {@link HttpSyncableService} with given port and {@link HttpEntityHandler}.
     *
     * @param port              listenable server's port
     * @throws IOException rethrow from {@link HttpServer#HttpServer}
     */
    public HttpBasicService(final int port) throws IOException {
        super(Utility.configFrom(port));
        BasicConfigurator.configure();
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
        return new FileExchangeSession(socket, this);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }
}
