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
        final var acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final var config = new HttpServerConfig();
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

    /** Request method for HTTP server.
     * @param id - id request.
     * @param request - type of request.
     * @param session - current HTTP session.
     * */
    @Path("/v0/entity")
    public void response(@Param(value = "id", required = true) final String id,
                         @Param(value = "replicas", required = true) final String replicas,
                         @NotNull final Request request,
                         @NotNull final HttpSession session) {
        log.debug("Request handling : {}", id);
        if (id == null || id.isEmpty()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }

        final boolean proxied = request.getHeader(ResponseUtils.PROXY) != null;
        final ReplicasFactor replicasFactor = replicas == null ? quorum : ReplicasFactor.parser(replicas);
        if (replicasFactor.getAck() < 1
                || replicasFactor.getFrom() < replicasFactor.getAck()
                || replicasFactor.getFrom() > this.topology.all().size()){
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return;
        }
        final var key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
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

    @NotNull
    private Response formEntryResponse(@NotNull final Entry entry) {
        final Response result;
        switch (entry.getState()) {
            case PRESENT:
                result = ResponseUtils.nonemptyResponse(Response.OK, entry.getData());
                result.addHeader(ResponseUtils.TIMESTAMP + entry.getTimestamp());
                return result;
            case REMOVED:
                result = ResponseUtils.emptyResponse(Response.NOT_FOUND);
                result.addHeader(ResponseUtils.TIMESTAMP + entry.getTimestamp());
                return result;
            case ABSENT:
                return ResponseUtils.emptyResponse(Response.NOT_FOUND);
            default:
                throw new IllegalArgumentException("Wrong input data");
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
                        return !topology.isNotMe(node)
                                ? action.act()
                                : proxy(node, request);
                    } catch (IOException e) {
                        log.error("Action error: ", e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private Response get(@NotNull final ByteBuffer key) throws IOException {
        try {
            final Entry value = Entry.getEntry(key, dao);
            return formEntryResponse(value);
        } catch (NoSuchElementException e) {
            return ResponseUtils.emptyResponse(Response.NOT_FOUND);
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
                for (Response response : replication(() -> get(key), request, key, replicasFactor)) {
                    if (ResponseUtils.getStatus(response).equals(Response.OK)
                            || ResponseUtils.getStatus(response).equals(Response.NOT_FOUND)) {
                        final Entry resp = Entry.fromEntry(response);
                        result.add(resp);
                    }
                }
                if (result.size() < replicasFactor.getAck()) {
                    session.sendResponse(ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
                } else {
                    session.sendResponse(formEntryResponse(Entry.mergeValues(result)));
                }
            } catch (IOException e) {
               log.error("Replication error: ", e);
               ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }

    private Response put(@NotNull final ByteBuffer key,
                         @NotNull final byte[] bytes) throws IOException {
        final var body = ByteBuffer.wrap(bytes);
        dao.upsert(key, body);
        return ResponseUtils.emptyResponse(Response.CREATED);
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
            try {
                final List<Response> result = replication(() -> put(key, request.getBody()), request, key, replicasFactor)
                        .stream()
                        .filter(node -> ResponseUtils.getStatus(node).equals(Response.CREATED))
                        .collect(Collectors.toList());
                if (result.size() < replicasFactor.getAck()) {
                    session.sendResponse(ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
                } else {
                    session.sendResponse(ResponseUtils.emptyResponse(Response.CREATED));
                }
            } catch (IOException e) {
                log.error("Ack number is less than required: ", e);
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
            }
        });
    }

    private Response delete(@NotNull final ByteBuffer key) throws IOException {
        dao.remove(key);
        return ResponseUtils.emptyResponse(Response.ACCEPTED);
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
            try {
                final List<Response> result = replication(() -> delete(key), request, key, replicasFactor)
                        .stream()
                        .filter(node -> ResponseUtils.getStatus(node).equals(Response.ACCEPTED))
                        .collect(Collectors.toList());
                if (result.size() < replicasFactor.getAck()) {
                    session.sendResponse(ResponseUtils.emptyResponse(ResponseUtils.NOT_ENOUGH_REPLICAS));
                } else {
                    session.sendResponse(ResponseUtils.emptyResponse(Response.ACCEPTED));
                }
            } catch (IOException e) {
                log.error("Ack number is less than required: ", e);
                ResponseUtils.sendEmptyResponse(session, Response.INTERNAL_ERROR);
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
            request.addHeader(ResponseUtils.PROXY);
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
