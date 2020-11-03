package ru.mail.polis.service.nik27090;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpHelper {
    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);
    private static final String TIMESTAMP = "timestamp";
    private static final String TIMESTAMP2 = "Timestamp:";

    private static final String NOT_ENOUGH_REPLICAS = "Not enough replicas error with ack: {}, from: {}";

    /**
     * Calculate result response.
     *
     * @param session               - session
     * @param sizeNotFailedResponse - size good response
     * @param ackFrom               - ack/from
     * @param goodResponse          - good response
     */
    public void calculateResponse(final HttpSession session,
                                  final int sizeNotFailedResponse,
                                  final AckFrom ackFrom,
                                  final Response goodResponse) {
        if (sizeNotFailedResponse < ackFrom.getAck()) {
            log.error(NOT_ENOUGH_REPLICAS, ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        } else {
            sendResponse(session, goodResponse);
        }
    }

    /**
     * Add response in session.
     *
     * @param session  - current session
     * @param response - response of session
     */
    public void sendResponse(@NotNull final HttpSession session, @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error("Can't send response", e);
        }
    }

    /**
     * Convert CompletableFuture<Response> to Response, when complete
     *
     * @param session  - session
     * @param response - response
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void respond(
            @NotNull final HttpSession session,
            @NotNull final CompletableFuture<Response> response) {
        response.whenComplete((r, t) -> {
            if (t == null) {
                sendResponse(session, r);
            } else {
                sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        });
    }

    /**
     * Proxy request to node.
     *
     * @param node       - node
     * @param request    - request
     * @param httpClient - httpClient
     * @return - response from node
     */
    @NotNull
    public CompletableFuture<Response> proxy(
            @NotNull final String node,
            @NotNull final Request request,
            @NotNull final HttpClient httpClient) {
        final HttpRequest proxyRequest = requestForReplica(request, node);
        return httpClient
                .sendAsync(
                        proxyRequest,
                        HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::convertHttpResponse)
                .exceptionally(ex -> new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }

    @NotNull
    private HttpRequest requestForReplica(
            @NotNull final Request request,
            @NotNull final String node
    ) {
        final String uri = request.getURI();
        final String url = node + uri;

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Proxy-For", "True")
                .timeout(Duration.ofMillis(500));

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                requestBuilder.GET();
                break;
            case Request.METHOD_PUT:
                final byte[] bodyContent = request.getBody();
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(bodyContent));
                break;
            case Request.METHOD_DELETE:
                requestBuilder.DELETE();
                break;
            default:
                throw new IllegalStateException("Method " + request.getMethodName() + " not supported!");
        }

        return requestBuilder.build();
    }

    @NotNull
    private Response convertHttpResponse(final HttpResponse<byte[]> response) {
        if (response == null) {
            log.error("Can't convert response from replica");
            return new Response(Response.INTERNAL_ERROR);
        }
        final Response r = new Response(String.valueOf(response.statusCode()), response.body());
        r.addHeader(TIMESTAMP2 + response.headers().firstValue(TIMESTAMP).orElse("-1"));
        return r;
    }
}
