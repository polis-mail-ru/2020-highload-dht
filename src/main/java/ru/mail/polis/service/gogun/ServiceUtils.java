package ru.mail.polis.service.gogun;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.gogun.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static ru.mail.polis.service.gogun.AsyncServiceImpl.log;

final class ServiceUtils {

    /**
     * Util class.
     */
    private ServiceUtils() {
    }

    public static ByteBuffer getBuffer(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    public static byte[] getArray(final ByteBuffer buffer) {
        byte[] body;
        if (buffer.hasRemaining()) {
            body = new byte[buffer.remaining()];
            buffer.get(body);
        } else {
            body = Response.EMPTY;
        }

        return body;
    }

    static void getCompletableFutureGetResponses(
            final List<CompletableFuture<Entry>> responsesFutureGet,
            final ReplicasFactor localReplicasFactor,
            final HttpSession session,
            final ExecutorService executorService
    ) {
        Futures.atLeastAsync(localReplicasFactor.getAck(), responsesFutureGet).whenCompleteAsync((v, t) -> {
            try {
                if (v == null) {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    return;
                }

                final EntryMerger<Entry> entryMerger = new EntryMerger<>(v, localReplicasFactor.getAck());

                session.sendResponse(entryMerger.mergeGetResponses());
            } catch (IOException e) {
                log.error("error sending response", e);
            }
        }, executorService).isCancelled();
    }

    static void getCompletableFuturePutDeleteResponses(
            final Request request,
            final List<CompletableFuture<Response>> responsesFuture,
            final ReplicasFactor localReplicasFactor,
            final HttpSession session,
            final ExecutorService executorService
    ) {
        Futures.atLeastAsync(localReplicasFactor.getAck(), responsesFuture).whenCompleteAsync((v, t) -> {
            try {
                if (v == null) {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    return;
                }

                final EntryMerger<Response> entryMerger = new EntryMerger<>(v, localReplicasFactor.getAck());

                session.sendResponse(entryMerger.mergePutDeleteResponses(request));

            } catch (IOException e) {
                log.error("error sending response", e);
            }
        }, executorService).isCancelled();
    }

    /**
     * Method provides config for HttpServer.
     *
     * @param port       port
     * @param numWorkers threads in executor service
     * @return built config
     */
    @NotNull
    public static HttpServerConfig makeConfig(final int port, final int numWorkers) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        config.selectors = numWorkers;

        return config;
    }

    @NotNull
    static HttpRequest.Builder requestForRepl(@NotNull final String node,
                                              @NotNull final String id) {
        final String uri = node + "/v0/entity?id=" + id;
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("X-Proxy-For", "true")
                    .timeout(AsyncServiceImpl.TIMEOUT);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("uri error", e);
        }
    }

    static void sendServiceUnavailable(final HttpSession session, final Logger log) {
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
        } catch (IOException e) {
            log.error("Error sending response in method get", e);
        }
    }

    static boolean checkEndParam(final String end) {
        return end == null || end.isEmpty();
    }

    static Response handlePut(@NotNull final ByteBuffer key, @NotNull final Request request, final DAO dao) {
        try {
            dao.upsert(key, ServiceUtils.getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    static Entry handleGet(@NotNull final ByteBuffer key, final DAO dao) {
        final Value value;
        Entry entry;
        try {
            value = dao.getValue(key);
        } catch (IOException e) {
            log.error("Internal server error get", e);
            throw new IllegalStateException(e);
        } catch (NoSuchElementException e) {
            entry = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Status.ABSENT);
            return entry;
        }

        if (value.isTombstone()) {
            entry = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Status.REMOVED);
        } else {
            entry = new Entry(Entry.ABSENT, ServiceUtils.getArray(value.getData()), Status.PRESENT);
        }
        entry.setTimestamp(value.getTimestamp());

        return entry;
    }

    static Response handleDel(@NotNull final ByteBuffer key, final DAO dao) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
