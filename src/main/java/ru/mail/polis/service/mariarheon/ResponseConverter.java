package ru.mail.polis.service.mariarheon;

import one.nio.http.Response;

import java.net.http.HttpResponse;

public final class ResponseConverter {
    private ResponseConverter() {
        /* nothing */
    }

    /**
     * Converts response from one-nio implementation to java implementation.
     *
     * @param response one-nio response-object.
     * @return java response-object.
     */
    public static Response convert(final HttpResponse<byte[]> response) {
        if (response == null) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        final String statusCode = String.valueOf(response.statusCode());
        return new Response(statusCode, response.body());
    }
}
