package ru.mail.polis.service.gogun;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
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
    public static void getCompletableFutureOnResponse(final Supplier<Response> putRequest,
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

    public static CompletableFuture<Entry> getCompletableFutureOnResponse(final Supplier<Entry> putRequest,
                                                                          final Supplier<Entry> getRequest,
                                                                          final Supplier<Entry> deleteRequest,
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

    public static CompletableFuture<Entry> getCompletableFutureOnResponse(final String node,
                                                                          final String id,
                                                                          final Request request,
                                                                          final HttpClient client,
                                                                          final ExecutorService executorService) {
        HttpRequest requestForReplica;
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                requestForReplica = requestForRepl(node, id)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_GET:
                requestForReplica = requestForRepl(node, id).GET()
                        .build();
                return client.sendAsync(requestForReplica, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_DELETE:
                requestForReplica = requestForRepl(node, id).DELETE()
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            default:
                throw new IllegalStateException("Wrong request method");
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
}
