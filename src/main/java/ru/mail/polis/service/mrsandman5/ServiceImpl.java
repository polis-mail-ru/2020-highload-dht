package ru.mail.polis.service.mrsandman5;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.range.ServiceSession;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;
import ru.mail.polis.service.mrsandman5.replication.SimpleRequests;
import ru.mail.polis.utils.ByteUtils;
import ru.mail.polis.utils.FuturesUtils;
import ru.mail.polis.utils.RequestUtils;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final DAOImpl dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final ReplicasFactor quorum;
    @NotNull
    private final Map<String, HttpClient> httpClients;
    @NotNull
    private final SimpleRequests simpleRequests;
    @NotNull
    private final ExecutorService executor;

    /** Create new ServiceImpl instance.
     * @param port - target port.
     * @param topology - cluster topology
     * @param dao - custom LSM DAO.
     * @param executor - thread executor.
     * */
    @NotNull
    public static Service create(final int port,
                                 @NotNull final Topology<String> topology,
                                 @NotNull final DAO dao,
                                 @NotNull final ExecutorService executor) throws IOException {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return new ServiceImpl(config, topology, dao, executor);
    }

    /** Service constructor.
     * @param config - http-server config.
     * @param topology - cluster topology
     * @param dao - custom LSM DAO.
     * */
    private ServiceImpl(@NotNull final HttpServerConfig config,
                        @NotNull final Topology<String> topology,
                        @NotNull final DAO dao,
                        @NotNull final ExecutorService executor) throws IOException {
        super(config);
        this.topology = topology;
        this.dao = (DAOImpl) dao;
        this.executor = executor;
        this.simpleRequests = new SimpleRequests(this.dao, executor);
        this.quorum = ReplicasFactor.quorum(this.topology.all().size());
        final Map<String, HttpClient> temp = new HashMap<>();
        for (final String node : topology.others()) {
            temp.put(node, HttpClient.newBuilder().executor(executor).version(HttpClient.Version.HTTP_1_1).build());
        }
        this.httpClients = temp;
    }

    /** Process request to get value.
     * {@code 200, value} (value is found).
     * {@code 404} (value is not found).
     * {@code 201} (new value created).
     * {@code 202} (value deleted).
     * {@code 400} (invalid request).
     * {@code 405} (unexpected method).
     *
     * @param id       value id
     * @param expire       record ttl
     * @param replicas         replica to whom value belongs
     * @param request     HTTP request
     * @param session HTTP session
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @Param(value = "expires") final String expire,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) {
        if (id.isEmpty()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final boolean proxied = request.getHeader(ResponseUtils.PROXY) != null;
        final ReplicasFactor replicasFactor = RequestUtils.getReplicasFactor(session, topology, proxied, replicas, quorum);
        if (replicasFactor == null) {
            return;
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final Instant expireTime = RequestUtils.getExpire(request, expire, proxied);
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                respond(session, proxied ? simpleRequests.get(key) : replicasGet(id, replicasFactor, expireTime));
                break;
            case Request.METHOD_PUT:
                respond(session, proxied ? simpleRequests.put(key, request.getBody(), expireTime)
                                : replicasPut(id, request.getBody(), replicasFactor, expireTime));
                break;
            case Request.METHOD_DELETE:
                respond(session, proxied ? simpleRequests.delete(key) : replicasDelete(id, replicasFactor, expireTime));
                break;
            default:
                log.error("Non-supported request : {}", id);
                ResponseUtils.sendEmptyResponse(session, Response.METHOD_NOT_ALLOWED);
                break;
        }
    }

    /**
     * Process request to get range of values.
     *
     * @param start Start value
     * @param end End value
     * @param session HTTP session
     */
    @Path("/v0/entities")
    public void entities(@Param(value = "start", required = true) final String start,
                         @Param(value = "end") final String end,
                         @NotNull final HttpSession session) {
        if (start.isEmpty()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final ByteBuffer startBuffer = ByteUtils.getWrap(start);
        final ByteBuffer endBuffer = end == null ? null : ByteUtils.getWrap(end);
        try {
            final Iterator<Record> range = dao.range(startBuffer, endBuffer);
            ((ServiceSession) session).stream(range);
        } catch (IOException e) {
            log.error("Unable to stream range of values", e);
            ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
        }
    }

    private void respond(@NotNull final HttpSession session,
                         @NotNull final CompletableFuture<Response> response) {
        response.whenComplete((r, t) -> {
            if (t == null) {
                ResponseUtils.sendResponse(session, r);
            } else {
                final String code;
                if (t instanceof CompletionException) {
                    t = t.getCause();
                }
                code = t instanceof IllegalStateException ? ResponseUtils.NOT_ENOUGH_REPLICAS : Response.INTERNAL_ERROR;
                ResponseUtils.sendNonEmptyResponse(session, code, t.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        }).isCancelled();
    }

    @NotNull
    private CompletableFuture<Response> replicasGet(@NotNull final String id,
                                                    @NotNull final ReplicasFactor replicasFactor,
                                                    @NotNull final Instant expire) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Entry>> result = new ArrayList<>(replicasFactor.getFrom());
        for (final String node : topology.replicasFor(key, replicasFactor)) {
            if (topology.isMe(node)) {
                result.add(simpleRequests.getEntry(key));
            } else {
                result.add(ResponseUtils.getResponse(httpClients, node, id, expire));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .handle((res, ex) -> ex == null ? Entry.entriesToResponse(res)
                        : ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
    }

    @NotNull
    private CompletableFuture<Response> replicasPut(@NotNull final String id,
                                                    @NotNull final byte[] value,
                                                    @NotNull final ReplicasFactor replicasFactor,
                                                    @NotNull final Instant expire) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Response>> result = new ArrayList<>(replicasFactor.getFrom());
        for (final String node : topology.replicasFor(key, replicasFactor)) {
            if (topology.isMe(node)) {
                result.add(simpleRequests.put(key, value, expire));
            } else {
                result.add(ResponseUtils.putResponse(httpClients, node, id, value, expire));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .handle((res, ex) -> ex == null ? ResponseUtils.emptyResponse(Response.CREATED)
                        : ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
    }

    @NotNull
    private CompletableFuture<Response> replicasDelete(@NotNull final String id,
                                                       @NotNull final ReplicasFactor replicasFactor,
                                                       @NotNull final Instant expire) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Response>> result = new ArrayList<>(replicasFactor.getFrom());
        for (final String node : topology.replicasFor(key, replicasFactor)) {
            if (topology.isMe(node)) {
                result.add(simpleRequests.delete(key));
            } else {
                result.add(ResponseUtils.deleteResponse(httpClients, node, id, expire));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .handle((res, ex) -> ex == null ? ResponseUtils.emptyResponse(Response.ACCEPTED)
                        : ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
    }

    /** Request method for status return.
     * @param session - current HTTP session.
     * */
    @Path("/v0/status")
    public Response status(@NotNull final HttpSession session) {
        return ResponseUtils.emptyResponse(Response.OK);
    }

    @Override
    public HttpSession createSession(@NotNull final Socket socket) {
        return new ServiceSession(socket, this);
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
