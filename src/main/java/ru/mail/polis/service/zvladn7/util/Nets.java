package ru.mail.polis.service.zvladn7.util;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;

public final class Nets {

    private static final Logger log = LoggerFactory.getLogger(Nets.class);
    public static final String PROXY_REQUEST_HEADER = "X-Proxy-To-Node";
    public static final int TIMEOUT = 500;

    private Nets() {
    }

    /**
     * Provide BodyPublisher by http method name.
     * @param request - request which was sent to the server
     * @return BodyPublisher for building request for async client.
     */
    public static HttpRequest.BodyPublisher getBodyPublisher(@NotNull final Request request) {
        switch (request.getMethodName()) {
            case "PUT":
                return HttpRequest.BodyPublishers.ofByteArray(request.getBody());
            case "DELETE":
                return HttpRequest.BodyPublishers.noBody();
            default:
                throw new IllegalArgumentException("Unknown method");
        }
    }

    /**
     * Transform method name to response.
     * @param methodName - name of Http request
     * @return Response corresponding to the method name
     */
    public static Response getChangeResponse(final String methodName) {
        return new Response(getResponseStatus(methodName), Response.EMPTY);
    }

    private static String getResponseStatus(@NotNull final String methodName) {
        switch (methodName) {
            case "PUT":
                return Response.CREATED;
            case "DELETE":
                return Response.ACCEPTED;
            default:
                throw new IllegalArgumentException("Unknown method");
        }
    }

    /**
     * Prepare builder for specialization.
     * @param node - node identifier
     * @param id - id of value which was requested
     * @return request builder
     */
    public static HttpRequest.Builder requestBuilderFor(@NotNull final String node,
                                                         @NotNull final String id) {
        try {
            return HttpRequest.newBuilder()
                    .uri(provideURI(node, id))
                    .timeout(Duration.ofMillis(TIMEOUT).dividedBy(2))
                    .header(PROXY_REQUEST_HEADER, node);
        } catch (URISyntaxException e) {
            log.error("Cannot construct URI on proxy request building on node {} with id: {}", node, id);
            throw new IllegalArgumentException("Failed to create URI", e);
        }
    }

    private static URI provideURI(@NotNull final String node,
                                  @NotNull final String id) throws URISyntaxException {
        return new URI(node + "/v0/entity?id=" + id);
    }

}
