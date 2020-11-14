package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.server.RejectedSessionException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomExecutor;
import ru.mail.polis.service.stasyanoi.StreamingSession;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    @Override
    public HttpSession createSession(Socket socket) {
        return new StreamingSession(socket, this);
    }

    protected void internalRun(final String idParam, final HttpSession session,
                             final Supplier<Response> responseSupplier) {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = responseSupplier.get();
        }
        try {
            session.sendResponse(responseHttp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    protected List<Response> getReplicaResponses(final Request request, final int node, final int fromOtherReplicas) {
        final Map<Integer, String> tempNodeMapping = new TreeMap<>(nodeIndexToUrlMapping);
        tempNodeMapping.remove(node);
        return getResponsesFromReplicas(tempNodeMapping, fromOtherReplicas, request);
    }

    protected List<Response> getResponsesFromReplicas(final Map<Integer, String> tempNodeMapping, final int from,
                                                    final Request request) {
        final List<String> urls = new ArrayList<>(tempNodeMapping.values()).subList(0, from);
        final List<Response> responses = new CopyOnWriteArrayList<>();
        final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (final String url : urls) {
            final CompletableFuture<Void> completableFuture = asyncHttpClient.sendAsync(
                    Util.getJavaRequest(request, url),
                    HttpResponse.BodyHandlers.ofByteArray()).thenApplyAsync(Util::getOneNioResponse)
                    .handleAsync(Util::filterResponse).thenAcceptAsync(responses::add);
            completableFutures.add(completableFuture);
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();
        return responses;
    }

    protected Response routeRequestToRemoteNode(final Request request, final int node,
                                              final Map<Integer, String> nodeMapping) {
        try {
            return Util.getOneNioResponse(asyncHttpClient.send(Util.getJavaRequest(request,nodeMapping.get(node)),
                    HttpResponse.BodyHandlers.ofByteArray()));
        } catch (InterruptedException | IOException e) {
            return Util.responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }
}
