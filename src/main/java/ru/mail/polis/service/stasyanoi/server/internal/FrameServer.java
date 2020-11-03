package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FrameServer extends PutGetDeleteMethodServer {

    /**
     * Server config without endpoints.
     *
     * @param dao - dao to use.
     * @param config - config
     * @param topology - nodes.
     * @throws IOException - thrown when IO errors encountered.
     */
    public FrameServer(final DAO dao,
                       final HttpServerConfig config,
                       final Set<String> topology) throws IOException {
        super(dao, config, topology);
    }

    @Override
    public synchronized void start() {
        super.start();
        dao.open();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
            executorService.shutdown();
            executorService.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Default handler for unmapped requests.
     *
     * @param request - unmapped request
     * @param session - session object
     * @throws IOException - if input|output exceptions occur within the method
     */
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = Util.responseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response status() {
        return Util.responseWithNoBody(Response.OK);
    }
}
