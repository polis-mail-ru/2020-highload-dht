package ru.mail.polis.service;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.mail.polis.service.ReplicationServiceUtils.syncValues;

final class FuturesHandler {
    private static final String PROXY_HEADER = "X-OK-Proxy: True";
    boolean isProxied;

    FuturesHandler(final boolean isForwardedRequest, @NotNull final DAO dao) {
        this.isProxied = isForwardedRequest;
    }

    static HttpRequest.Builder setProxyHeader(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(1))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    Response futureGet(
            final List<Value> values,
            final AtomicInteger atomicInteger,
            final List<CompletableFuture<Value>> futures,
            final int count
    ) throws IOException {
        for (final CompletableFuture<Value> future : futures) {
            try {
                if (future.isCompletedExceptionally()) {
                    continue;
                };
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

    Response futureUpsert(final AtomicInteger atomicInteger,
                          final int count,
                          final List<CompletableFuture<Response>> futures) throws IOException {
        atomicInteger.set(incrementAtomic(atomicInteger, 201, futures));
        if (atomicInteger.get() == futures.size() || atomicInteger.get() >= count) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    Response futureDelete(
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

    private int incrementAtomic(
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
