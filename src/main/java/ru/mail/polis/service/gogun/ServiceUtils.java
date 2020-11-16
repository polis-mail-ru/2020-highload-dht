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
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

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

    /**
     * Method provides selecting action for method type.
     *
     * @param putRequest    action for put request
     * @param getRequest    action for get request
     * @param deleteRequest action for delete request
     * @param method        request type
     * @param session       http session
     * @throws IOException send response exception
     */
    public static void selector(final Supplier<Response> putRequest,
                                final Supplier<Response> getRequest,
                                final Supplier<Response> deleteRequest,
                                final int method,
                                final HttpSession session) throws IOException {
        switch (method) {
            case Request.METHOD_PUT:
                session.sendResponse(putRequest.get());
                break;
            case Request.METHOD_GET:
                session.sendResponse(getRequest.get());
                break;
            case Request.METHOD_DELETE:
                session.sendResponse(deleteRequest.get());
                break;
            default:
                session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                break;
        }
    }

    public static CompletableFuture<Response> selector(final Supplier<Response> putRequest,
                                                       final Supplier<Response> getRequest,
                                                       final Supplier<Response> deleteRequest,
                                                       final int method,
                                                       final ExecutorService executorService) {
        switch (method) {
            case Request.METHOD_PUT:
                return CompletableFuture.supplyAsync(putRequest, executorService);
            case Request.METHOD_GET:
                return CompletableFuture.supplyAsync(getRequest, executorService);
            case Request.METHOD_DELETE:
                return CompletableFuture.supplyAsync(deleteRequest, executorService);
            default:
                return null;
        }
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

    static Response handlePut(@NotNull final ByteBuffer key,
                              @NotNull final Request request,
                              @NotNull final DAO dao,
                              @NotNull final Logger log) {
        try {
            dao.upsert(key, ServiceUtils.getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    static Response handleGet(@NotNull final ByteBuffer key,
                              @NotNull final DAO dao,
                              @NotNull final Logger log) {
        final Value value;
        Response response;
        try {
            value = dao.getValue(key);
        } catch (IOException e) {
            log.error("Internal server error get", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
            response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + AsyncServiceImpl.ABSENT);
            return response;
        }

        if (value.isTombstone()) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            response = Response.ok(ServiceUtils.getArray(value.getData()));
        }
        response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + value.getTimestamp());

        return response;
    }

    static Response handleDel(@NotNull final ByteBuffer key,
                              @NotNull final DAO dao,
                              @NotNull final Logger log) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
