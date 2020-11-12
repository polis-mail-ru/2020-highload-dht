package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomExecutor;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BaseFunctionalityServer extends ConstantsServer {

    /**
     * Server for basic functionality methods.
     *
     * @param dao - dao.
     * @param config - config.
     * @param topology - topology.
     * @throws IOException - IOException.
     */
    public BaseFunctionalityServer(final DAO dao, final HttpServerConfig config, final Set<String> topology)
            throws IOException {
        super(dao, config, topology);
    }

    @Override
    public synchronized void start() {
        logger.info("start " + thisNodeIndex);
        super.start();
        executorService = CustomExecutor.getExecutor();
        dao.open();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        logger.info("stop " + thisNodeIndex);
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
