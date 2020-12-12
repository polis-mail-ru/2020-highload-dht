package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * Class for creating requests.
 */
public final class RequestFactory {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private RequestFactory() {
        /* nothing */
    }

    /**
     * Create the request with timestamp header.
     *
     * @param uri - destination uri.
     * @param requestMethod - request method (GET, POST, etc.).
     * @param body - body of the request.
     * @param timestamp - timestamp value.
     * @return - created HTTP-request.
     */
    public static HttpRequest create(final URI uri, final int requestMethod,
                                     final byte[] body, final long timestamp) {
        final var builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT);
        switch (requestMethod) {
            case Request.METHOD_GET:
                builder.GET();
                break;
            case Request.METHOD_PUT:
                builder.PUT(HttpRequest.BodyPublishers.ofByteArray(body));
                break;
            case Request.METHOD_DELETE:
                builder.DELETE();
                break;
            default:
                return null;
        }
        builder.setHeader(Util.TIMESTAMP_HEADER, String.valueOf(timestamp));
        return builder.build();
    }
}
