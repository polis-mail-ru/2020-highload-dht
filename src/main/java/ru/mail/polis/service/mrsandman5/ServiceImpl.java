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
import ru.mail.polis.service.mrsandman5.client.AsyncClient;
import ru.mail.polis.service.mrsandman5.client.Client;
import ru.mail.polis.service.mrsandman5.client.LocalClient;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;
import ru.mail.polis.service.mrsandman5.replication.SimpleRequests;
import ru.mail.polis.utils.ByteUtils;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static ru.mail.polis.utils.ResponseUtils.PROXY;

public final class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final Topology<String> topology;
    private final ReplicasFactor quorum;
    private final Map<String, Client> httpClients;
    @NotNull
    private final SimpleRequests simpleRequests;

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
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.selectors = workersCount;

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
        final DAOImpl serviceDao = (DAOImpl) dao;
        this.simpleRequests = new SimpleRequests(serviceDao);
        this.quorum = ReplicasFactor.quorum(topology.all().size());
        this.httpClients = topology.others()
                .stream()
                .collect(toMap(identity(), this::createClient));
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
        if (id == null || id.isEmpty()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final boolean proxied = request.getHeader(PROXY) != null;
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
                                : replicasGet(session, request, id, replicasFactor));
                break;
            case Request.METHOD_PUT:
                respond(session,
                        proxied ? simpleRequests.put(key, request.getBody())
                                : replicasPut(session, request, id, replicasFactor));
                break;
            case Request.METHOD_DELETE:
                respond(session,
                        proxied ? simpleRequests.delete(key)
                                : replicasDelete(session, request, id, replicasFactor));
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
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }

    private List<Response> replication(@NotNull final Action local,
                                       @NotNull final String requestName,
                                       @NotNull final String id,
                                       @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        return topology.replicasFor(key, replicasFactor)
                .stream()
                .map(node -> {
                    try {
                        return topology.isMe(node)
                                ? local.act()
                                : proxy.act();
                    } catch (IOException e) {
                        log.error("Action error: ", e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private void replicasGet(@NotNull final HttpSession session,
                             @NotNull final String id,
                             @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        try {
            final Collection<CompletableFuture<Entry>> result =
                    new ArrayList<>(replicasFactor.getFrom());
            for (final Response response : replication(() ->
                    simpleRequests.get(key), id, replicasFactor)) {
                if (ResponseUtils.getStatus(response).equals(Response.OK)
                        || ResponseUtils.getStatus(response).equals(Response.NOT_FOUND)) {
                    final CompletableFuture<Entry> resp =
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    return Entry.responseToEntry(response);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }, workers);
                    result.add(resp);
                }
            }
            if (result.size() < replicasFactor.getAck()) {
                ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
            } else {
                ResponseUtils.sendResponse(session, Entry.entriesToResponse(result));
            }
        } catch (IOException e) {
            log.error("Replication error: ", e);
            ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
        }
    }

    private void replicasPut(@NotNull final HttpSession session,
                             @NotNull final Request request,
                             @NotNull final String id,
                             @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final List<Response> result = replication(() ->
                simpleRequests.put(key, request.getBody()), id, replicasFactor)
                .stream()
                .filter(node -> ResponseUtils.getStatus(node).equals(Response.CREATED))
                .collect(Collectors.toList());
        if (result.size() < replicasFactor.getAck()) {
            ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
        } else {
            ResponseUtils.sendEmptyResponse(session, Response.CREATED);
        }
    }

    private void replicasDelete(@NotNull final HttpSession session,
                                @NotNull final String id,
                                @NotNull final ReplicasFactor replicasFactor) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        final List<Response> result = replication(() -> simpleRequests.delete(key), id, replicasFactor)
                .stream()
                .filter(node -> ResponseUtils.getStatus(node).equals(Response.ACCEPTED))
                .collect(Collectors.toList());
        if (result.size() < replicasFactor.getAck()) {
            ResponseUtils.sendEmptyResponse(session, ResponseUtils.NOT_ENOUGH_REPLICAS);
        } else {
            ResponseUtils.sendEmptyResponse(session, Response.ACCEPTED);
        }
    }

    /** Request method for status return.
     * @param session - current HTTP session.
     * */
    @Path("/v0/status")
    public Response status(@NotNull final HttpSession session) {
        return ResponseUtils.emptyResponse(Response.OK);
    }

    /*private Response proxy(@NotNull final String node,
                           @NotNull final String id) {
        try {
            request.addHeader(PROXY + node);
            return httpClients.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException | IOException e) {
            log.error("Unable to proxy request", e);
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }*/

    /*@NotNull
    private static HttpClient createHttpClient(@NotNull final String node) {
        return new HttpClient(new ConnectionString(node + ResponseUtils.TIMEOUT));
    }*/

    @NotNull
    private Client createClient(@NotNull final String node) {
        if (topology.isMe(node)) {
            return new LocalClient(workers, simpleRequests);
        }
        return new AsyncClient(node, workers);
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) {
        ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
    }

    /*private void asyncExecute(@NotNull final HttpSession session,
                              @NotNull final Action action) {
        asyncExecute(() -> {
            try {
                ResponseUtils.sendResponse(session, action.act());
            } catch (IOException e) {
                log.error("Unable to create response", e);
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }*/

    @FunctionalInterface
    private interface Action {
        @NotNull
        Response act() throws IOException;
    }

}
