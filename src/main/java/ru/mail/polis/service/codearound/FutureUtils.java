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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  class which methods are taken to utility function for future-supporting service.
 */
public final class FutureUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationLsm.class);
    public static final String GET_COMPLETION_ERROR_LOG = "Future return error when running GET request handler";
    public static final String UPSERT_COMPLETION_ERROR_LOG = "Future return error when running PUT request handler";
    public static final String DELETE_COMPLETION_ERROR_LOG = "Future return error when running DELETE request handler";
    private static final String PROXY_HEADER = "X-OK-Proxy: True";

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
                .timeout(Duration.ofSeconds(ServiceFactory.TIMEOUT))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    /**
     * async handler to respond GET request.
     *
     * @param values - collection of values to be sent by HTTP response
     * @param nodes - array of cluster-belonging node IDs
     * @param futures - collection of future responses
     * @param count - cluster-wide success quorum to send response
     * @param isForwardedRequest - true if incoming request header indicates
     *                             invocation of proxy-providing method on a previous node
     * @return - HTTP response
     */
    public static Response execGetWithFutures(final List<Value> values,
                                              final List<CompletableFuture<HttpResponse<byte[]>>> futures,
                                              final String[] nodes,
                                              final int count,
                                              final boolean isForwardedRequest) throws IOException {
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
                LOGGER.error(UPSERT_COMPLETION_ERROR_LOG);
            }
        }
        if (quant.get() >= futures.size() || quant.get() >= count) {
            return RepliServiceUtils.processResponses(nodes, values, isForwardedRequest);
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
        if (quant.get() >= futures.size() || quant.get() >= count) {
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
        if (quant.get() >= futures.size() || quant.get() >= count) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * increment number of futures for response.
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

    public static ByteBuffer getKeyFromRequest(@NotNull final Request req) {
        final String key = req.getParameter("id=");
        return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
    }
}
