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
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;
import ru.mail.polis.service.mrsandman5.replication.SimpleRequests;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

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
     * @param executor - thread pool executor.
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
        final DAOImpl serviceDao = (DAOImpl) dao;
        this.simpleRequests = new SimpleRequests(serviceDao);
        this.executor = executor;
        this.quorum = ReplicasFactor.quorum(topology.all().size());
        this.httpClients = topology.others()
                .stream()
                .collect(toMap(identity(), ServiceImpl::createHttpClient));
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
        if (id == null || id.isEmpty()) {
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
                replicasGet(session, request, key, proxied, replicasFactor);
                break;
            case Request.METHOD_PUT:
                replicasPut(session, request, key, proxied, replicasFactor);
                break;
            case Request.METHOD_DELETE:
                replicasDelete(session, request, key, proxied, replicasFactor);
                break;
            default:
                log.error("Non-supported request : {}", id);
                ResponseUtils.sendEmptyResponse(session, Response.METHOD_NOT_ALLOWED);
                break;
        }
    }

    private List<Response> replication(@NotNull final ResponseUtils.Action action,
                                       @NotNull final Request request,
                                       @NotNull final ByteBuffer key,
                                       @NotNull final ReplicasFactor replicasFactor) {
        return topology.replicasFor(key, replicasFactor)
                .stream()
                .map(node -> {
                    try {
                        return topology.isMe(node)
                                ? action.act()
                                : proxy(node, request);
                    } catch (IOException e) {
                        log.error("Action error: ", e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private void replicasGet(@NotNull final HttpSession session,
                             @NotNull final Request request,
                             @NotNull final ByteBuffer key,
                             final boolean proxied,
                             @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> simpleRequests.get(key));
            return;
        }
        asyncExecute(() -> {
            try {
                final List<Entry> result = new ArrayList<>();
                for (final Response response : replication(() ->
                        simpleRequests.get(key), request, key, replicasFactor)) {
                    if (ResponseUtils.getStatus(response).equals(Response.OK)
                            || ResponseUtils.getStatus(response).equals(Response.NOT_FOUND)) {
                        final Entry resp = Entry.responseToEntry(response);
                        result.add(resp);
                    }
                }
                if (result.size() < replicasFactor.getAck()) {
                    ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
                } else {
                    ResponseUtils.sendResponse(session, Entry.entryToResponse(Entry.mergeEntries(result)));
                }
            } catch (IOException e) {
               log.error("Replication error: ", e);
               ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }

    private void replicasPut(@NotNull final HttpSession session,
                             @NotNull final Request request,
                             @NotNull final ByteBuffer key,
                             final boolean proxied,
                             @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> simpleRequests.put(key, request.getBody()));
            return;
        }
        asyncExecute(() -> {
            final List<Response> result = replication(() ->
                    simpleRequests.put(key, request.getBody()), request, key, replicasFactor)
                    .stream()
                    .filter(node -> ResponseUtils.getStatus(node).equals(Response.CREATED))
                    .collect(Collectors.toList());
            if (result.size() < replicasFactor.getAck()) {
                ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
            } else {
                ResponseUtils.sendEmptyResponse(session, Response.CREATED);
            }
        });
    }

    private void replicasDelete(@NotNull final HttpSession session,
                                @NotNull final Request request,
                                @NotNull final ByteBuffer key,
                                final boolean proxied,
                                @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> simpleRequests.delete(key));
            return;
        }
        asyncExecute(() -> {
            final List<Response> result = replication(() -> simpleRequests.delete(key), request, key, replicasFactor)
                    .stream()
                    .filter(node -> ResponseUtils.getStatus(node).equals(Response.ACCEPTED))
                    .collect(Collectors.toList());
            if (result.size() < replicasFactor.getAck()) {
                ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
            } else {
                ResponseUtils.sendEmptyResponse(session, Response.ACCEPTED);
            }
        });
    }

    /** Request method for status return.
     * @param session - current HTTP session.
     * */
    @Path("/v0/status")
    public Response status(@NotNull final HttpSession session) {
        return ResponseUtils.emptyResponse(Response.OK);
    }

    private Response proxy(@NotNull final String node,
                           @NotNull final Request request) {
        try {
            request.addHeader(ResponseUtils.PROXY + node);
            return httpClients.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException | IOException e) {
            log.error("Unable to proxy request", e);
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    @NotNull
    private static HttpClient createHttpClient(@NotNull final String node) {
        return new HttpClient(new ConnectionString(node + ResponseUtils.TIMEOUT));
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
    }

    private void asyncExecute(@NotNull final HttpSession session,
                              @NotNull final ResponseUtils.Action action) {
        executor.execute(() -> {
            try {
                ResponseUtils.sendResponse(session, action.act());
            } catch (IOException e) {
                log.error("Unable to create response", e);
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
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
