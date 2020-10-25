package ru.mail.polis.service.zvladn7;

import com.google.common.primitives.Longs;
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
import ru.mail.polis.dao.zvladn7.exceptions.DeletedValueException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

class ServiceHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceHelper.class);
    private static final String PROXY_REQUEST_HEADER = "X-Proxy-To-Node";

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
        handleOrProxy(key, request, session, () -> {
            final Value value;
            try {
                value = dao.getValue(key);
                log.debug("Value successfully got!");
                return getLocalResponse(value);
            } catch (IOException e) {
                log.error("Internal error. Can't get value with key: {}", id, e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            } catch (NoSuchElementException e) {
                log.info("Value with key: {} was not found", id, e);
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }, (responses, replicasHolder) -> ConflictResolver.resolveGetAndSend(responses, replicasHolder, session));
    }

    void handleDelete(
            @NotNull final String id,
            @NotNull final HttpSession session,
            @NotNull final Request request) throws IOException {
        final ByteBuffer key = wrapString(id);
        handleOrProxy(key, request, session, () -> {
            try {
                dao.remove(key);
                log.debug("Value successfully deleted!");
            } catch (IOException e) {
                log.error("Internal error. Can't delete value with key: {}", id, e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }, (responses, replicasHolder) -> ConflictResolver.resolveChangesAndSend(responses, replicasHolder, session, true));
    }

    void handleUpsert(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final HttpSession session) throws IOException {
        final ByteBuffer key = wrapString(id);
        handleOrProxy(key, request, session, () -> {
            final ByteBuffer value = wrapArray(request.getBody());
            try {
                dao.upsert(key, value);
                log.debug("Value successfully upserted!");
            } catch (IOException e) {
                log.error("Internal error. Can't insert or update value with key: {}", id, e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
            return new Response(Response.CREATED, Response.EMPTY);
        }, (responses, replicasHolder) -> ConflictResolver.resolveChangesAndSend(responses, replicasHolder, session, false));
    }

    private Response getLocalResponse(final Value value) {
        final byte[] bytes = Longs.toByteArray(value.getTimestamp());
        try {
            final byte[] body = toBytes(value.getData(), bytes);
            return Response.ok(body);
        } catch (DeletedValueException ex) {
            return new Response(Response.NOT_FOUND, bytes);
        }
    }

    private List<Response> proxy(@NotNull final Set<String> nodesForResponse,
                                 @NotNull final Request request) {
        log.info("Start proxy");
        log.debug("Proxy request: {} from {} to {}", request.getMethodName(), topology.local(), nodesForResponse);
        final List<Response> responses = new ArrayList<>();
        nodesForResponse.forEach(node -> {
            try {
                request.addHeader(PROXY_REQUEST_HEADER + ": " + node);
                responses.add(clients.get(node).invoke(request));
            } catch (IOException | InterruptedException | HttpException | PoolException e) {
                log.error("Cannot proxy request!", e);
                responses.add(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        });
        return responses;
    }

    private void handleOrProxy(final ByteBuffer key,
                               final Request request,
                               final HttpSession session,
                               final LocalExecutor localExecutor,
                               final Resolver resolver) throws IOException {
        final String id = request.getParameter("id=");
        final ReplicasHolder replicasHolder = parseReplicasParameter(request.getParameter("replicas="));
        log.debug("{} request with mapping: /v0/entity with: key={}", request.getMethodName(), id);
        log.debug("ack: {}, from: {}", replicasHolder.ack, replicasHolder.from);
        if (id.isEmpty()) {
            sendEmptyIdResponse(session, request.getMethodName());
            return;
        }
        if (isInvalidReplicationFactor(replicasHolder)) {
            sendInvalidRFResponse(session, replicasHolder);
            return;
        }
        final String header = request.getHeader(PROXY_REQUEST_HEADER);
        log.debug("Header: {}", header);
        final Set<String> nodesForResponse = topology.nodesForKey(key, replicasHolder.from);
        Response localResponse = null;
        log.debug(nodesForResponse.toString());
        if (topology.isLocal(nodesForResponse)) {
            nodesForResponse.remove(topology.local());
            localResponse = localExecutor.execute();
            if (header != null) {
                session.sendResponse(localResponse);
            }
        }
        List<Response> responses;
        if (header == null) {
            responses = proxy(nodesForResponse, request);
            if (localResponse != null) {
                responses.add(localResponse);
            }
            final List<Response> responsesWithoutErrors = responses.stream()
                    .filter(response -> response.getStatus() != 500)
                    .collect(Collectors.toList());
            resolver.resolve(responsesWithoutErrors, replicasHolder);
        }
    }

    private boolean isInvalidReplicationFactor(@NotNull final ReplicasHolder replicasHolder) {
        return replicasHolder.ack == 0 || replicasHolder.ack > replicasHolder.from;
    }

    private ReplicasHolder parseReplicasParameter(final String replicas) {
        if (replicas == null) {
            return new ReplicasHolder(topology.size() / ServiceTopology.VIRTUAL_NODES_PER_NODE);
        } else {
            return new ReplicasHolder(replicas);
        }
    }

    private static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    private static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
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

    private byte[] toBytes(final ByteBuffer data, final byte[] bytes) {
        final byte[] dataBytes = toBytes(data);
        final byte[] mergedBytes = new byte[dataBytes.length + bytes.length];
        log.info("Before: {}", Arrays.toString(mergedBytes));
        System.arraycopy(dataBytes, 0, mergedBytes, 0, dataBytes.length);
        System.arraycopy(bytes, 0, mergedBytes, dataBytes.length, bytes.length);
        log.info("After: {}", Arrays.toString(mergedBytes));
        return mergedBytes;
    }

    private static void sendEmptyIdResponse(final HttpSession session,
                                            final String methodName) throws IOException {
        log.info("Empty key was provided in {} method!", methodName);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private static void sendInvalidRFResponse(final HttpSession session,
                                              final ReplicasHolder replicasHolder) throws IOException {
        log.info("Invalid replication factor with ack = {}, from = {}", replicasHolder.ack, replicasHolder.from);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

}
