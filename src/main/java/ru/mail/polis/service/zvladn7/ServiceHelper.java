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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

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
            @NotNull final Request request) throws IOException {
        final ByteBuffer key = wrapString(id);
        handleOrProxy(key, id, request, session, () -> {
            log.debug("Not from cache with id: {}", id);
            final ByteBuffer value;
            try {
                value = dao.get(key);
            } catch (NoSuchElementException e) {
                log.info("Value with key: {} was not found", id, e);
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                return;
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            final byte[] body = toBytes(value);

            session.sendResponse(Response.ok(body));
        });
    }

    void handleDelete(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request) throws IOException {
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
            @NotNull final HttpSession session) throws IOException {
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

    void proxy(@NotNull final String nodeForResponse,
               @NotNull final Request request,
               @NotNull final HttpSession session) throws IOException {
        log.debug("Proxy request: {} from {} to {}", request.getMethodName(), topology.local(), nodeForResponse);
        try {
            request.addHeader("X-Proxy-For: " + nodeForResponse);
            session.sendResponse(clients.get(nodeForResponse).invoke(request));
        } catch (IOException | InterruptedException | HttpException | PoolException e) {
            log.error("Cannot proxy request!", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
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
        if (topology.isLocal(nodeForResponse)) {
            processor.process();
        } else {
            proxy(nodeForResponse, request, session);
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
