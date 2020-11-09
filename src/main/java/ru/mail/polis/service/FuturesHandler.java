package ru.mail.polis.service;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

final class FuturesHandler {
    private static final String PROXY_HEADER = "X-OK-Proxy: True";

    boolean isProxied;
    @NotNull
    private final DAO dao;

    FuturesHandler(final boolean isForwardedRequest, @NotNull final DAO dao) {
        this.isProxied = isForwardedRequest;
        this.dao = dao;
    }

    /**
     * enables building any HTTP request based on featuring pattern.
     *
     * @param node - cluster-belonging node ID
     * @param req  - HTTP request
     * @return HTTP request build pattern
     */
    static HttpRequest.Builder setRequestPattern(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(1))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    Response execGetWithFutures(
            final List<Value> values,
            final AtomicInteger atomicInteger,
            final List<CompletableFuture<HttpResponse<byte[]>>> futures,
            final Set<String> nodes,
            final int count
    ) throws IOException {
        for (final CompletableFuture<HttpResponse<byte[]>> future : futures) {
            try {
                if (future.isCompletedExceptionally()) continue;
                if (future.get().body().length == 0) {
                    values.add(Value.resolveMissingValue());
                } else if (future.get().statusCode() != 500) {
                    values.add(Value.composeFromBytes(future.get().body()));
                }
                atomicInteger.incrementAndGet();
            } catch (ExecutionException | InterruptedException exc) {
                throw new IOException("Error handling futures", exc);
            }
        }
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return Value.toResponse(nodes, values, isProxied);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * async handler to respond PUT request.
     *
     * @param atomicInteger - quantity of success responses
     * @param count         - cluster-wide success quorum to send response
     * @param futures       - collection of future responses
     * @return - HTTP response
     */
    Response execUpsertWithFutures(final AtomicInteger atomicInteger,
                                   final int count,
                                   final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        atomicInteger.set(incrementFutureCount(atomicInteger, 201, futures));
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * async handler to respond DELETE request.
     *
     * @param atomicInteger - quantity of success responses
     * @param count         - cluster-wide success quorum to send response
     * @param futures       - collection of future responses
     * @return - HTTP response
     */
    Response execDeleteWithFutures(
            final AtomicInteger atomicInteger,
            final int count,
            final List<CompletableFuture<HttpResponse<byte[]>>> futures
    ) {
        atomicInteger.set(incrementFutureCount(atomicInteger, 202, futures));
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * increment number of futures for response.
     *
     * @param atomicInteger - quantity of success responses
     * @param returnCode    - HTTP return code
     * @param futures       - collection of future responses
     * @return - HTTP response
     */
    private int incrementFutureCount(
            final AtomicInteger atomicInteger,
            final int returnCode,
            final List<CompletableFuture<HttpResponse<byte[]>>> futures
    ) {
        for (final var future : futures) {
            try {
                if (future.isCompletedExceptionally()) {
                    continue;
                }
                if (future.get().statusCode() == returnCode) {
                    atomicInteger.incrementAndGet();
                }
            } catch (ExecutionException | InterruptedException ignore) {

            }
        }
        return atomicInteger.get();
    }

    CompletableFuture<HttpResponse<byte[]>> handleLocal(@NotNull final Request request) {
        return CompletableFuture.supplyAsync(() -> {
            DummyHttpResponseBuilder builder = new DummyHttpResponseBuilder();
            try {
                switch (request.getMethod()) {
                    case Request.METHOD_GET:
                        final Response responses = getValue(getKeyFromRequest(request));
                        return builder.setCode(responses.getStatus())
                                .setBody(responses.getBody());
                    case Request.METHOD_PUT:
                        upsertValue(getKeyFromRequest(request), request);
                        return builder.setCode(201);
                    case Request.METHOD_DELETE:
                        deleteValue(getKeyFromRequest(request));
                        return builder.setCode(202);
                    default:
                        return builder.setCode(405);
                }
            } catch (IOException e) {
                return builder.setCode(404);
            }
        });
    }

    @NotNull
    private Response getValue(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] value = composeFromBytes(key);
            return new Response(Response.OK, value);
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private void upsertValue(
            @NotNull final ByteBuffer key,
            @NotNull final Request request
    ) throws IOException {
        dao.upsertValue(key, ByteBuffer.wrap(request.getBody()));
    }

    private void deleteValue(@NotNull final ByteBuffer key) throws IOException {
        dao.removeValue(key);
    }

    private static ByteBuffer getKeyFromRequest(@NotNull final Request req) {
        final String strKey = req.getParameter("id=");
        return ByteBuffer.wrap(strKey.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] composeFromBytes(@NotNull final ByteBuffer key) throws IOException {
        final Value value = dao.getValue(key);
        if (value.isValueMissing()) {
            throw new NoSuchElementException("Value not found");
        }
        return value.getValueBytes();
    }
}
