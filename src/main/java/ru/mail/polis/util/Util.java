package ru.mail.polis.util;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

public final class Util {
    public static final String HANDLE_ME = "target";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private Util() {
        /* Add private constructor to prevent instantiation */
    }

    /**
     * This converts ByteBuffer to byte array.
     *
     * @param buffer ByteBuffer
     * @return array bytes
     */
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);
        return arr;
    }

    /**
     * This wraps byte array into ByteBuffer.
     *
     * @param arr byte array
     * @return ByteBuffer with shifted bytes
     */
    public static ByteBuffer fromShiftedArray(@NotNull final byte[] arr) {
        final byte[] copy = Arrays.copyOf(arr, arr.length);

        int i = 0;
        while (i < copy.length) {
            copy[i] = (byte) (Byte.toUnsignedInt(copy[i]) + Byte.MIN_VALUE);
            i++;
        }

        return ByteBuffer.wrap(copy);
    }

    /**
     * This takes array from a ByteBuffer and performs all bytes shift by MIN_VALUE.
     *
     * @param buffer ByteBuffer
     * @return array with shifted bytes
     */
    public static byte[] toShiftedArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);

        int i = 0;
        while (i < arr.length) {
            arr[i] = (byte) (Byte.toUnsignedInt(arr[i]) - Byte.MIN_VALUE);
            i++;
        }

        return arr;
    }

    /**
     * Mark request for local processing.
     * @param request - request to mark
     * @return - Request
     */
    public static Request markTargetRequest(final Request request) {
        if (request.getParameter(HANDLE_ME) != null) {
            return request;
        }

        final String newURI = request.getURI() + "&" + HANDLE_ME + "=";
        final Request modifiedRequest = new Request(request.getMethod(), newURI, request.isHttp11());

        Arrays.stream(
                request.getHeaders(), 0, request.getHeaderCount()).forEach(modifiedRequest::addHeader
        );
        modifiedRequest.setBody(request.getBody());
        return modifiedRequest;
    }

    /**
     * Converts response from java.net.http to one.nio.http format.
     * @param response - java.net.http response
     * @return one.nio.http Response
     */
    public static Response convertResponse(final HttpResponse<byte[]> response) {
        if (response == null) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        final String statusCode = String.valueOf(response.statusCode());
        return new Response(statusCode, response.body());
    }

    /**
     * Converts request from java.net.http to one.nio.http format.
     * @param request - java.net.http request
     * @param host    - current host
     * @return        - one.nio.http Request
     */
    public static HttpRequest convertRequest(final Request request, final String host) {
        final String query = request.getURI();
        final String wholeURIString = host + query;
        final URI uri = URI.create(wholeURIString);
        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT);

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                builder.GET();
                break;
            case Request.METHOD_PUT:
                final byte[] requestBody = request.getBody();
                final HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(requestBody);
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

    /**
     * Wraps string to ByteBuffer.
     * @param id - string to wrap
     * @return ByteBuffer
     */
    public static ByteBuffer toByteBuffer(final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }
}
