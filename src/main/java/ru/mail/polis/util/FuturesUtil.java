package ru.mail.polis.util;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Value;

import java.io.IOException;
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

    /**
     * Redirects request to target node.
     * @param node - target node id
     * @param req - one-nio request
     * @return HttpRequest.Builder
     */
    public static HttpRequest.Builder setProxyHeader(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(1))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    /**
     * Processes futures of GET request.
     * @param values - future values that will be completed
     * @param atomicInteger - atomic counter to count successes
     * @param futures - futures list
     * @param count - ack target value
     * @return - one-nio Response
     * @throws IOException - something went wrong
     */
    public static Response futureGet(
            final List<Value> values,
            final AtomicInteger atomicInteger,
            final List<CompletableFuture<Value>> futures,
            final int count
    ) throws IOException {
        for (final CompletableFuture<Value> future : futures) {
            try {
                if (future.isCompletedExceptionally()) continue;
                values.add(future.get());
                atomicInteger.incrementAndGet();
            } catch (ExecutionException | InterruptedException exc) {
                throw new IOException("Error handling futures", exc);
            }
        }
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return Value.toResponse(syncValues(values));
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * Processes futures of PUT request.
     * @param atomicInteger - atomic counter to count successes
     * @param count - ack target value
     * @param futures - futures list
     * @return - one-nio Response
     * @throws IOException - something went wrong
     */
    public static Response futureUpsert(final AtomicInteger atomicInteger,
                                        final int count,
                                        final List<CompletableFuture<Response>> futures) throws IOException {
        atomicInteger.set(incrementAtomic(atomicInteger, 201, futures));
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * Processes futures of DELETE request.
     * @param atomicInteger - atomic counter to count successes
     * @param count - ack target value
     * @param futures - futures list
     * @return - one-nio Response
     * @throws IOException - something went wrong
     */
    public static Response futureDelete(
            final AtomicInteger atomicInteger,
            final int count,
            final List<CompletableFuture<Response>> futures
    ) throws IOException {
        atomicInteger.set(incrementAtomic(atomicInteger, 202, futures));
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * This does atomic computation.
     * @param atomicInteger - atomic counter to count successes
     * @param returnCode - target response code
     * @param futures - futures list
     * @return - resulting counter value
     * @throws IOException - something went wrong
     */
    private static int incrementAtomic(
            final AtomicInteger atomicInteger,
            final int returnCode,
            final List<CompletableFuture<Response>> futures
    ) throws IOException {
        for (final var future : futures) {
            try {
                if (future.isCompletedExceptionally()) {
                    continue;
                }
                if (future.get().getStatus() == returnCode) {
                    atomicInteger.incrementAndGet();
                }
            } catch (ExecutionException | InterruptedException exc) {
                throw new IOException("Error incrementing futures count", exc);
            }
        }
        return atomicInteger.get();
    }

}
