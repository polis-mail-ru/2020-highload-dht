package ru.mail.polis.service.mrsandman5;

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
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.handlers.DeleteBodyHandler;
import ru.mail.polis.service.mrsandman5.handlers.GetBodyHandler;
import ru.mail.polis.service.mrsandman5.handlers.PutBodyHandler;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;
import ru.mail.polis.service.mrsandman5.replication.SimpleRequests;
import ru.mail.polis.utils.ByteUtils;
import ru.mail.polis.utils.FuturesUtils;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        this.quorum = ReplicasFactor.quorum(topology.all().size());
        final Map<String, HttpClient> temp = new HashMap<>();
        for (final String node : topology.others()) {
            temp.put(node, HttpClient.newBuilder()
                    .executor(executor)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build());
        }
        this.httpClients = temp;
    }

    /** Interact with service.
     * {@code 200, value} (value is found).
     * {@code 404} (value is not found).
     * {@code 201} (new value created).
     * {@code 202} (value deleted).
     * {@code 400} (invalid request).
     * {@code 405} (unexpected method).
     */
    @Path("/v0/entity")
    public void response(@Param(value = "id", required = true) final String id,
                         @Param(value = "replicas") final String replicas,
                         @NotNull final Request request,
                         @NotNull final HttpSession session) {
        log.debug("Request handling : {}", id);
        if (id.isEmpty()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final boolean proxied = request.getHeader(ResponseUtils.PROXY) != null;
        final ReplicasFactor replicasFactor = proxied || replicas == null ? quorum : ReplicasFactor.parser(replicas);
        if (replicasFactor.getAck() < 1
                || replicasFactor.getFrom() < replicasFactor.getAck()
                || replicasFactor.getFrom() > this.topology.all().size()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                respond(session,
                        proxied ? simpleRequests.get(key)
                                : Objects.requireNonNull(replicasGet(id, replicasFactor)));
                break;
            case Request.METHOD_PUT:
                respond(session,
                        proxied ? simpleRequests.put(key, request.getBody())
                                : Objects.requireNonNull(replicasPut(id, request.getBody(), replicasFactor)));
                break;
            case Request.METHOD_DELETE:
                respond(session,
                        proxied ? simpleRequests.delete(key)
                                : Objects.requireNonNull(replicasDelete(id, replicasFactor)));
                break;
            default:
                log.error("Non-supported request : {}", id);
                ResponseUtils.sendEmptyResponse(session, Response.METHOD_NOT_ALLOWED);
                break;
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
                if (t instanceof IllegalStateException) {
                    code = ResponseUtils.NOT_ENOUGH_REPLICAS;
                } else {
                    code = Response.INTERNAL_ERROR;
                }
                ResponseUtils.sendNonEmptyResponse(session,
                        code,
                        t.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        }).isCancelled();
    }

    private CompletableFuture<Response> replicasGet(@NotNull final String id,
                                                    @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Entry>> result = new ArrayList<>(replicasFactor.getFrom());
        final Set<String> topologies;
        try {
            topologies = topology.replicasFor(key, replicasFactor);
        } catch (IllegalArgumentException e) {
            log.error("Topology error", e);
            return null;
        }
        for (final String node : topologies) {
            if (topology.isMe(node)) {
                result.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return Entry.entryFromBytes(key, dao);
                            } catch (IOException e) {
                                throw new RuntimeException("Get future error", e);
                            }
                        }, executor));
            } else {
                final HttpRequest request = ResponseUtils.requestForReplica(node, id).GET().build();
                result.add(httpClients.get(node)
                        .sendAsync(request, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executor));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .thenApply(Entry::entriesToResponse);
    }

    private CompletableFuture<Response> replicasPut(@NotNull final String id,
                                                    @NotNull final byte[] value,
                                                    @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Response>> result = new ArrayList<>(replicasFactor.getFrom());
        final Set<String> topologies;
        try {
            topologies = topology.replicasFor(key, replicasFactor);
        } catch (IllegalArgumentException e) {
            log.error("Topology error", e);
            return null;
        }
        for (final String node : topologies) {
            if (topology.isMe(node)) {
                result.add(simpleRequests.put(key, value));
            } else {
                final HttpRequest request = ResponseUtils.requestForReplica(node, id)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(value))
                        .build();
                result.add(httpClients.get(node)
                        .sendAsync(request, PutBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> ResponseUtils.emptyResponse(Response.CREATED), executor));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .thenApply(r -> ResponseUtils.emptyResponse(Response.CREATED));
    }

    private CompletableFuture<Response> replicasDelete(@NotNull final String id,
                                                       @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final Collection<CompletableFuture<Response>> result = new ArrayList<>(replicasFactor.getFrom());
        final Set<String> topologies;
        try {
            topologies = topology.replicasFor(key, replicasFactor);
        } catch (IllegalArgumentException e) {
            log.error("Topology error", e);
            return null;
        }
        for (final String node : topologies) {
            if (topology.isMe(node)) {
                result.add(simpleRequests.delete(key));
            } else {
                final HttpRequest request = ResponseUtils.requestForReplica(node, id).DELETE().build();
                result.add(httpClients.get(node)
                        .sendAsync(request, DeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(r -> ResponseUtils.emptyResponse(Response.ACCEPTED), executor));
            }
        }
        return FuturesUtils.atLeastAsync(result, replicasFactor.getAck(), executor)
                .thenApply(r -> ResponseUtils.emptyResponse(Response.ACCEPTED));
    }

    /** Request method for status return.
     * @param session - current HTTP session.
     * */
    @Path("/v0/status")
    public Response status(@NotNull final HttpSession session) {
        return ResponseUtils.emptyResponse(Response.OK);
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
            log.error("Can't stop the executor", ie);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
