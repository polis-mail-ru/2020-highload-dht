package ru.mail.polis.service.zvladn7;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.zvladn7.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

class ServiceHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceHelper.class);
    private final Topology<String> topology;
    private final Map<String, HttpClient> clients;
    private final DAO dao;

    /**
     * Helper for asynchronous server implementation.
     *
     * @param topology - topology of local node
     * @param dao      - DAO implemenation
     */
    ServiceHelper(@NotNull final Topology<String> topology,
                  @NotNull final DAO dao) {
        this.topology = topology;
        this.dao = dao;
        this.clients = new HashMap<>();
        for (final String node : topology.nodes()) {
            if (topology.isLocal(node)) {
                continue;
            }
            final ConnectionString connectionString = new ConnectionString(node + "?timeout=1000");
            final HttpClient client = new HttpClient(connectionString);
            if (clients.put(node, client) != null) {
                log.error("Cannot start server. Duplicate node with connection string: {}", node);
                throw new IllegalStateException("Duplicate node");
            }
        }
    }

    void handleGet(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request,
            final String replicas) throws IOException {
        final ByteBuffer key = wrapString(id);

        log.debug("{} request with mapping: /v0/entity with: key={}", request.getMethodName(), id);
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, request.getMethodName());
            return;
        }
        final ReplicasHolder replicasHolder;
        if (replicas == null) {
            replicasHolder = new ReplicasHolder(topology.size());
        } else {
            replicasHolder = new ReplicasHolder(replicas);
        }
        final Set<String> nodesForResponse = topology.nodesForKey(key, replicasHolder.from);
        log.info("id: {}", request.getParameter("id="));
        log.info("replicas: {}", request.getParameter("replicas="));
        Response localResponse = null;
        if (topology.isLocal(nodesForResponse)) {
            nodesForResponse.remove(topology.local());
//            processor.process();
            log.debug("Not from cache with id: {}", id);
            final Value value;
            try {
                value = dao.getValue(key);
            } catch (NoSuchElementException e) {
                log.info("Value with key: {} was not found", id, e);
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                return;
            }
            final byte[] body = toBytes(value.getData());

            localResponse = Response.ok(body);
        }
        List<Response> responses = proxy(nodesForResponse, request);
        if (localResponse != null) {
            responses.add(localResponse);
        }
        ConflictResolver.resolveGetAndSend(responses, replicasHolder, session);


//        handleOrProxy(key, id, request, session, () -> {
//            log.debug("Not from cache with id: {}", id);
//            final ByteBuffer value;
//            try {
//                value = dao.get(key);
//            } catch (NoSuchElementException e) {
//                log.info("Value with key: {} was not found", id, e);
//                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
//                return;
//            } catch (IOException e) {
//                sendInternalErrorResponse(session, id, e);
//                return;
//            }
//            final byte[] body = toBytes(value);
//
//            session.sendResponse(Response.ok(body));
//        });
    }

    void handleDelete(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request, String replicas) throws IOException {
        final ByteBuffer key = wrapString(id);
        handleOrProxy(key, id, request, session, () -> {
            try {
                dao.remove(key);
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        });
    }

    void handleUpsert(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final HttpSession session, String replicas) throws IOException {
        final ByteBuffer key = wrapString(id);
        handleOrProxy(key, id, request, session, () -> {
            final ByteBuffer value = wrapArray(request.getBody());
            try {
                dao.upsert(key, value);
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        });
    }

    private List<Response> proxy(@NotNull final Set<String> nodesForResponse,
                                 @NotNull final Request request) {
        log.debug("Proxy request: {} from {} to {}", request.getMethodName(), topology.local(), nodesForResponse);
        final List<Response> responses = new ArrayList<>();
        nodesForResponse.forEach(node -> {
            try {
                request.addHeader("X-Proxy-For: " + nodesForResponse);
                responses.add(clients.get(node).invoke(request));
            } catch (IOException | InterruptedException | HttpException | PoolException e) {
                log.error("Cannot proxy request!", e);
                responses.add(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        });
        return responses;
    }

    private void handleOrProxy(final ByteBuffer key,
                               final String id,
                               final Request request,
                               final HttpSession session,
                               final Processor processor) throws IOException {
        log.debug("{} request with mapping: /v0/entity with: key={}", request.getMethodName(), id);
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, request.getMethodName());
            return;
        }
        final String nodeForResponse = topology.nodeFor(key);
        log.info("----------------");
        for (String s : topology.nodesForKey(key, 2)) {
            log.info("handleOrProxy: {}", s);
        }
        log.info("id: {}", request.getParameter("id="));
        log.info("replicas: {}", request.getParameter("replicas="));
        if (topology.isLocal(nodeForResponse)) {
            processor.process();
        } else {
            proxy(nodeForResponse, request);
        }
    }

    private static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    private static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    private static void sendInternalErrorResponse(final HttpSession session,
                                                  final String id,
                                                  final Exception e) throws IOException {
        log.error("Internal error. Can't insert or update value with key: {}", id, e);
        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }

    private static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    private static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }

        return Response.EMPTY;
    }

    private static void sendEmptyIdResponse(final HttpSession session, final String methodName) throws IOException {
        log.info("Empty key was provided in {} method!", methodName);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

}
