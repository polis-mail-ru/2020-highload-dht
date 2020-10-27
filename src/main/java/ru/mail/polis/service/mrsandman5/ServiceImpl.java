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
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final DAOImpl dao;
    @NotNull
    private final Topology<String> topology;
    private final ReplicasFactor quorum;
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
        this.dao = (DAOImpl) dao;
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
        log.debug("Request handling : {}", id);
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

    private List<Response> replication(@NotNull final Action action,
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

    /**
     * Get value.
     * {@code 200, value} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    private Response get(@NotNull final ByteBuffer key) {
        try {
            final Entry value = Entry.entryFromBytes(key, dao);
            return Entry.entryToResponse(value);
        } catch (NoSuchElementException e) {
            return ResponseUtils.emptyResponse(Response.NOT_FOUND);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    private void replicasGet(@NotNull final HttpSession session,
                             @NotNull final Request request,
                             @NotNull final ByteBuffer key,
                             final boolean proxied,
                             @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> get(key));
            return;
        }
        asyncExecute(() -> {
            try {
                final List<Entry> result = new ArrayList<>();
                for (final Response response : replication(() -> get(key), request, key, replicasFactor)) {
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

    /**
     * Put value.
     * {@code 201} (new value created).
     * {@code 500} (internal server error occurred).
     */
    private Response put(@NotNull final ByteBuffer key,
                         @NotNull final byte[] bytes) {
        try {
            final ByteBuffer body = ByteBuffer.wrap(bytes);
            dao.upsert(key, body);
            return ResponseUtils.emptyResponse(Response.CREATED);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    private void replicasPut(@NotNull final HttpSession session,
                             @NotNull final Request request,
                             @NotNull final ByteBuffer key,
                             final boolean proxied,
                             @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> put(key, request.getBody()));
            return;
        }
        asyncExecute(() -> {
            final List<Response> result = replication(() -> put(key, request.getBody()), request, key, replicasFactor)
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

    /**
     * Delete value by the key.
     * {@code 202} (value deleted).
     * {@code 500} (internal server error occurred).
     */
    private Response delete(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
            return ResponseUtils.emptyResponse(Response.ACCEPTED);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    private void replicasDelete(@NotNull final HttpSession session,
                                @NotNull final Request request,
                                @NotNull final ByteBuffer key,
                                final boolean proxied,
                                @NotNull final ReplicasFactor replicasFactor) {
        if (proxied) {
            asyncExecute(session, () -> delete(key));
            return;
        }
        asyncExecute(() -> {
            final List<Response> result = replication(() -> delete(key), request, key, replicasFactor)
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
                              @NotNull final Action action) {
        asyncExecute(() -> {
            try {
                ResponseUtils.sendResponse(session, action.act());
            } catch (IOException e) {
                log.error("Unable to create response", e);
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }

    @FunctionalInterface
    private interface Action {
        @NotNull
        Response act() throws IOException;
    }

}
