package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

public final class RequestConverter {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private RequestConverter() {
        /* nothing */
    }

    /**
     * Converts request from one-nio implementation to java implementation.
     *
     * @param request one-nio request-object.
     * @return java request-object.
     */
    public static HttpRequest convert(final Request request, final String host) {
        final var query = request.getURI();
        final var wholeURIString = host + query;
        final var uri = URI.create(wholeURIString);
        final var builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT);
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                builder.GET();
                break;
            case Request.METHOD_PUT:
                final var bodyContent = request.getBody();
                final var body = HttpRequest.BodyPublishers.ofByteArray(bodyContent);
                builder.PUT(body);
                break;
            case Request.METHOD_DELETE:
                builder.DELETE();
                break;
            default:
                return null;
        }
        return builder.build();
    }
}
