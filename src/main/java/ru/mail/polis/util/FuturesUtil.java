package ru.mail.polis.util;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Value;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.mail.polis.service.ReplicationServiceUtils.syncValues;

public final class FuturesUtil {
    private static final String PROXY_HEADER = "X-OK-Proxy: True";

    private FuturesUtil() {
        /* Add private constructor to prevent instantiation */
    }

    public static Response futureGet(final List<Value> values,
                                     final List<CompletableFuture<Value>> futures,
                                     final int ack) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        for (final CompletableFuture<Value> future : futures) {
            try {
                if (future.isCompletedExceptionally()) continue;
                values.add(future.get());
                atomicInteger.incrementAndGet();
                if (atomicInteger.get() == futures.size() || atomicInteger.get() == ack) {
                    return Value.toResponse(syncValues(values));
                }
            } catch (ExecutionException | InterruptedException ignore) {

            }
        }

        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * Processes futures of DELETE request.
     *
     * @param ack     - ack target value
     * @param futures - futures list
     * @return - one-nio Response
     */
    public static Response futureDelete(
            final int ack,
            final List<CompletableFuture<Response>> futures
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final boolean res = count(ack, atomicInteger, 202, futures);
        if (res) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * Processes futures of PUT request.
     *
     * @param ack     - ack target value
     * @param futures - futures list
     * @return - one-nio Response
     */
    public static Response futureUpsert(
            final int ack,
            final List<CompletableFuture<Response>> futures
    ) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final boolean res = count(ack, atomicInteger, 201, futures);

        if (res) {
            return new Response(Response.CREATED, Response.EMPTY);
        }
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * @param ack           - ack target value
     * @param atomicInteger - atomic counter
     * @param returnCode    - response's target return code
     * @param futures       - futures list
     * @return - resulting counter value
     */
    private static boolean count(
            final int ack,
            final AtomicInteger atomicInteger,
            final int returnCode,
            final List<CompletableFuture<Response>> futures
    ) {
        for (final CompletableFuture<Response> future : futures) {
            try {
                final Response result = future.get();
                if (result.getStatus() == returnCode) {
                    atomicInteger.incrementAndGet();
                    if (atomicInteger.get() == futures.size() || atomicInteger.get() == ack) {
                        return true;
                    }
                }
            } catch (ExecutionException | InterruptedException ignore) {

            }
        }
        return false;
    }

    /**
     * Redirects request to target node.
     *
     * @param node - target node id
     * @param req  - one-nio request
     * @return HttpRequest.Builder
     */
    public static HttpRequest.Builder setProxyHeader(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(1))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }
}
