package ru.mail.polis.service.mrsandman5;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.mrsandman5.clustering.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> httpClients;

    /** Create new ServiceImpl instance.
     * @param port - target port.
     * @param topology - cluster topology
     * @param dao - custom LSM DAO.
     * @param workersCount - thread workers.
     * */
    @NotNull
    public static Service create(final int port,
                                 @NotNull final Topology<String> topology,
                                 @NotNull final DAO dao,
                                 final int workersCount) throws IOException {
        final var acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final var config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.minWorkers = 1;
        config.maxWorkers = workersCount;

        return new ServiceImpl(config, topology, dao);
    }

    /** Service constructor.
     * @param config - http-server config.
     * @param topology - cluster topology
     * @param dao - custom LSM DAO.
     * */
    private ServiceImpl(@NotNull final HttpServerConfig config,
                       @NotNull final Topology<String> topology,
                       @NotNull final DAO dao) throws IOException {
        super(config);
        this.topology = topology;
        this.dao = dao;
        this.httpClients = topology.others().stream()
                .collect(toMap(identity(), ServiceImpl::createHttpClient));
    }

    /** Request method for HTTP server.
     * @param id - id request.
     * @param request - type of request.
     * @param session - current HTTP session.
     * */
    @Path("/v0/entity")
    public void response(@Param(value = "id", required = true) final String id,
                         @NotNull final Request request,
                         @NotNull final HttpSession session) {
        log.debug("Request handling : {}", id);
        if (id.isEmpty()) {
            sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }

        final var key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final var node = topology.primaryFor(key);
        if (topology.isNotMe(node)) {
            asyncExecute(session, () -> proxy(node, request));
            return;
        }
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                asyncExecute(session, () -> get(key));
                break;
            case Request.METHOD_PUT:
                asyncExecute(session, () -> put(key, request.getBody()));
                break;
            case Request.METHOD_DELETE:
                asyncExecute(session, () -> delete(key));
                break;
            default:
                log.error("Non-supported request : {}", id);
                sendEmptyResponse(session, Response.METHOD_NOT_ALLOWED);
                break;
        }
    }

    private Response get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final var value = dao.get(key);
            return new Response(Response.OK, toByteArray(value));
        } catch (NoSuchElementException e) {
            return emptyResponse(Response.NOT_FOUND);
        }
    }

    private Response put(@NotNull final ByteBuffer key,
                         final byte[] bytes) throws IOException {
        if (bytes == null) {
            return emptyResponse(Response.BAD_REQUEST);
        }
        final var body = ByteBuffer.wrap(bytes);
        dao.upsert(key, body);
        return emptyResponse(Response.CREATED);
    }

    private Response delete(@NotNull final ByteBuffer key) throws IOException {
        dao.remove(key);
        return emptyResponse(Response.ACCEPTED);
    }

    /** Request method for status return.
     * @param session - current HTTP session.
     * */
    @Path("/v0/status")
    public Response status(@NotNull final HttpSession session) {
        return emptyResponse(Response.OK);
    }

    private Response proxy(@NotNull final String node,
                           @NotNull final Request request) throws IOException {
        try {
            return httpClients.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            log.error("Unable to proxy request", e);
            return emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    @NotNull
    private static HttpClient createHttpClient(@NotNull final String node) {
        return new HttpClient(new ConnectionString(node + "?timeout=100"));
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        sendEmptyResponse(session, Response.BAD_REQUEST);
    }

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void asyncExecute(@NotNull final HttpSession session,
                              @NotNull final ResponseSupplier supplier) {
        asyncExecute(() -> {
            try {
                sendResponse(session, supplier.supply());
            } catch (IOException e) {
                log.error("Unable to create response", e);
            }
        });
    }

    private static void sendResponse(@NotNull final HttpSession session,
                                     @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            try {
                log.error("Unable to send response", e);
                session.sendError(Response.INTERNAL_ERROR, null);
            } catch (IOException ex) {
                log.error("Unable to send error", e);
            }
        }
    }

    private static void sendEmptyResponse(@NotNull final HttpSession session,
                                          @NotNull final String code) {
        sendResponse(session, emptyResponse(code));
    }

    @NotNull
    private static Response emptyResponse(@NotNull final String code) {
        return new Response(code, Response.EMPTY);
    }

    @FunctionalInterface
    private interface ResponseSupplier {
        @NotNull
        Response supply() throws IOException;
    }
}
