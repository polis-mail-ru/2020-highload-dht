package ru.mail.polis.service.codearound;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.ServiceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  class which methods are taken to utility function for future-supporting service.
 */
public final class FutureUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationLsm.class);
    private static final String PROXY_HEADER = "X-OK-Proxy: True";
    public static final String GET_COMPLETION_ERROR_LOG = "Future return error when running GET request handler";
    public static final String UPSERT_COMPLETION_ERROR_LOG = "Future return error when running PUT request handler";
    public static final String DELETE_COMPLETION_ERROR_LOG = "Future return error when running DELETE request handler";

    /**
     * class const.
     */
    private FutureUtils() {
      //  Not supposed to be instantiated
    }

    /**
     * enables building any HTTP request based on featuring pattern.
     *
     * @param node - cluster-belonging node ID
     * @param req - HTTP request
     * @return HTTP request build pattern
     */
    public static HttpRequest.Builder setRequestPattern(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(ServiceFactory.TIMEOUT_SECONDS))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    /**
     * async handler to respond GET request.
     *
     * @param values - collection of values to be sent by HTTP response
     * @param nodes - array of cluster-belonging node IDs
     * @param futures - collection of future responses
     * @param count - cluster-wide success quorum to send response
     * @param req - HTTP request
     * @return HTTP response
     */
    public static Response execGetWithFutures(final List<Value> values,
                                              final List<CompletableFuture<HttpResponse<byte[]>>> futures,
                                              final String[] nodes,
                                              final int count,
                                              @NotNull final Request req,
                                              final ExecutorService exec) throws IOException {
        final AtomicInteger quant = new AtomicInteger(0);
        for (final var future : futures) {
            try {
                if (future.get().body().length == 0) {
                    values.add(Value.resolveMissingValue());
                } else if (future.get().statusCode() != 500) {
                    values.add(Value.getValueFromBytes(future.get().body()));
                }
                quant.incrementAndGet();
            } catch (ExecutionException | InterruptedException exc) {
                LOGGER.error(GET_COMPLETION_ERROR_LOG);
            }
        }
        if (quant.get() == futures.size() || quant.get() == count) {
            return RepliServiceUtils.processResponses(nodes, values, req);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * async handler to respond PUT request.
     *
     * @param count - cluster-wide success quorum to send response
     * @param futures - collection of future responses
     * @return HTTP response
     */
    public static Response execUpsertWithFutures(final int count,
                                                 final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        final AtomicInteger quant = new AtomicInteger(0);
        incrementFutureCount(quant, 201, futures);
        if (quant.get() == futures.size() || quant.get() == count) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * async handler to respond DELETE request.
     *
     * @param count - cluster-wide success quorum to send response
     * @param futures - collection of future responses
     * @return HTTP response
     */
    public static Response execDeleteWithFutures(final int count,
                                                 final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        final AtomicInteger quant = new AtomicInteger(0);
        incrementFutureCount(quant, 202, futures);
        if (quant.get() == futures.size() || quant.get() == count) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * increments number of futures for response.
     *
     * @param quant - quantity of success responses
     * @param returnCode - HTTP return code
     * @param futures - collection of future responses
     */
    private static void incrementFutureCount(final AtomicInteger quant,
                                             final int returnCode,
                                             final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        for (final var future : futures) {
            try {
                if (future.get().statusCode() == returnCode) {
                    quant.incrementAndGet();
                }
            } catch (ExecutionException | InterruptedException exc) {
                LOGGER.error(DELETE_COMPLETION_ERROR_LOG);
            }
        }
        quant.get();
    }

    /**
     * completes (either normally or exceptionally) the future collection contents up to ack successes precisely.
     *
     * @param ack - cluster-wide success quorum to send response
     * @param futures - collection of future responses
     * @param exec - service to run threads
     * @return collection of future responses complete to advance to http client
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static <T> CompletableFuture<Collection<T>> completeOnceAckSuccesses(
            final int ack,
            @NotNull final Collection<CompletableFuture<T>> futures,
            @NotNull final ExecutorService exec) {

        assert 0 < ack && ack <= futures.size();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();
        final AtomicInteger successesRemaining = new AtomicInteger(ack);
        final AtomicInteger failuresRemaining = new AtomicInteger(futures.size() - ack + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();

        futures.forEach(f -> f.whenCompleteAsync((v, t) -> {
            if (t == null) {
                results.add(v);
                if (successesRemaining.decrementAndGet() == 0) {
                    future.complete(results);
                }
            } else {
                if (failuresRemaining.decrementAndGet() == 0) {
                    future.completeExceptionally(t);
                }
            }
        }, exec));

        return future;
    }
}
