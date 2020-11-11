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

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureUtils.class);
    private static final String PROXY_HEADER = "X-OK-Proxy: True";
    private static final String GET_FUTURE_ERROR_LOG = "Failed obtaining a future result";
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
     * @param ack - success quorum to send response
     * @param req - HTTP request
     * @return HTTP response
     */
    public static Response execGetWithFutures(final List<Value> values,
                                              final List<CompletableFuture<HttpResponse<byte[]>>> futures,
                                              final String[] nodes,
                                              final int ack,
                                              @NotNull final Request req) throws IOException {
        final AtomicInteger quant = new AtomicInteger(0);
        for (final var future : futures) {
            try {
                var result = future.get();
                if (result.body().length == 0) {
                    values.add(Value.resolveMissingValue());
                } else if (result.statusCode() != 500) {
                    values.add(Value.getValueFromBytes(future.get().body()));
                }
                quant.incrementAndGet();
                if (quant.get() == futures.size() || quant.get() == ack) {
                    return RepliServiceUtils.processResponses(nodes, values, req);
                }
            } catch (ExecutionException | InterruptedException exc) {
                LOGGER.error(GET_COMPLETION_ERROR_LOG);
            }
        }

        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * dual async handler to respond both PUT and DELETE requests.
     *
     * @param req - HTTP request to be handled
     * @param ack - success quorum to send response
     * @param futures - collection of future responses
     * @return HTTP response
     */
    public static Response execDualWithFutures(@NotNull final Request req,
                                               final int ack,
                                               final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        final AtomicInteger quant = new AtomicInteger(0);
        final boolean res;
        if (req.getMethod() == Request.METHOD_PUT) {
            res = countSuccesses(ack, quant, 201, futures);
            if (res) {
                return new Response(Response.CREATED, Response.EMPTY);
            }
        } else if (req.getMethod() == Request.METHOD_DELETE) {
            res = countSuccesses(ack, quant, 202, futures);
            if (res) {
                return new Response(Response.ACCEPTED, Response.EMPTY);
            }
        }
        return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * increments number of futures for response.
     *
     * @param ack - success quorum to send response
     * @param quant - quantity of success responses
     * @param returnCode - HTTP return code suggested as completion success
     * @param futures - collection of future responses
     */
    private static boolean countSuccesses(final int ack,
                                          final AtomicInteger quant,
                                          final int returnCode,
                                          final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        for (final var future : futures) {
            try {
                var result = future.get();
                if (result.statusCode() == returnCode) {
                    quant.incrementAndGet();
                    if (quant.get() == futures.size() || quant.get() == ack) {
                        return true;
                    }
                }
            } catch (ExecutionException | InterruptedException exc) {
                LOGGER.error(GET_FUTURE_ERROR_LOG);
            }
        }
        return false;
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
