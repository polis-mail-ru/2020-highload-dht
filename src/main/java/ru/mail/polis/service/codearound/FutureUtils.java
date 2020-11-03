package ru.mail.polis.service.codearound;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.ServiceFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
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
    private boolean isForwardedRequest;
    @NotNull
    private final DAO dao;

    /**
     * class const.
     *
     * @param isForwardedRequest - true if incoming request header indicates
     *                             invocation of proxy-providing method on a previous node
     * @param dao - DAO instance
     */
    public FutureUtils(final boolean isForwardedRequest, @NotNull final DAO dao) {
        this.isForwardedRequest = isForwardedRequest;
        this.dao = dao;
    }

    /**
     * evaluates request proxy status in boolean terms.
     *
     * @return true if incoming request header indicates
     *          invocation of proxy-providing method on a previous node
     */
    public boolean isForwardedRequest() {
        return isForwardedRequest;
    }

    /**
     * request proxy status setter.
     *
     * @param isForwardedRequest - true if incoming request header indicates
     *                            invocation of proxy-providing method on a previous node
     */
    public void setForwardedRequest(final boolean isForwardedRequest) {
        this.isForwardedRequest = isForwardedRequest;
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
                .timeout(Duration.ofSeconds(ServiceFactory.CONNECTION_TIMEOUT))
                .setHeader("PROXY_HEADER", PROXY_HEADER);
    }

    /**
     * async handler to respond DELETE request.
     *
     * @param quant - quantity of success responses
     * @param count - cluster-wide success quorum to send response
     * @param futures - collection of future responses
     * @return - HTTP response
     */
    public Response execDeleteWithFutures(final AtomicInteger quant,
                                          final int count,
                                          final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        quant.set(incrementFutureCount(quant, 202, futures));
        if (quant.get() >= futures.size() || quant.get() >= count) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    /**
     * async handler to respond PUT request.
     *
     * @param quant - quantity of success responses
     * @param count - cluster-wide success quorum to send response
     * @param futures - collection of future responses
     * @return - HTTP response
     */
    public Response execUpsertWithFutures(final AtomicInteger quant,
                                          final int count,
                                          final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        quant.set(incrementFutureCount(quant, 201, futures));
        if (quant.get() >= futures.size() || quant.get() >= count) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
    }

    /**
     * async handler to respond GET request.
     *
     * @param values - collection of values to be sent by HTTP response
     * @param quant - quantity of success responses
     * @param nodes - array of cluster-belonging node IDs
     * @param futures - collection of future responses
     * @param count - cluster-wide success quorum to send response
     * @return - HTTP response
     */
    public Response execGetWithFutures(final List<Value> values,
                                       final AtomicInteger quant,
                                       final List<CompletableFuture<HttpResponse<byte[]>>> futures,
                                       final String[] nodes,
                                       final int count) throws IOException {
        for (final var future : futures) {
            try {
                if (future.isCompletedExceptionally()) continue;
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
     * increment number of futures for response.
     *
     * @param quant - quantity of success responses
     * @param returnCode - HTTP return code
     * @param futures - collection of future responses
     * @return - HTTP response
     */
    private int incrementFutureCount(final AtomicInteger quant,
                                     final int returnCode,
                                     final List<CompletableFuture<HttpResponse<byte[]>>> futures) {
        for (final var future : futures) {
            try {
                if (future.isCompletedExceptionally()) {
                    continue;
                }
                if (future.get().statusCode() == returnCode) {
                    quant.incrementAndGet();
                }
            } catch (ExecutionException | InterruptedException exc) {
                LOGGER.error(DELETE_COMPLETION_ERROR_LOG);
            }
        }
        return quant.get();
    }

    /**
     * async handler to run on the node requested (also by proxy) at the moment.
     *
     * @param req - HTTP request
     * @return collection of future responses
     */
    public CompletableFuture<HttpResponse<byte[]>> execLocalRequest(@NotNull final Request req) {
        return CompletableFuture.supplyAsync(() -> {
            AsyncConnectUtils response;
            try {
                switch (req.getMethod()) {
                    case Request.METHOD_GET:
                        final Response responses = getValue(getKeyFromRequest(req));
                        return new AsyncConnectUtils().setReturnCode(responses.getStatus())
                                .setBody(responses.getBody());
                    case Request.METHOD_PUT:
                        upsertValue(getKeyFromRequest(req), req);
                        return new AsyncConnectUtils().setReturnCode(201);
                    case Request.METHOD_DELETE:
                        deleteValue(getKeyFromRequest(req));
                        return new AsyncConnectUtils().setReturnCode(202);
                    default:
                        return new AsyncConnectUtils().setReturnCode(405);
                }
            } catch (IOException e) {
                response = new AsyncConnectUtils().setReturnCode(404);
            }
            return response;
        });
    }

    /**
     * Access dao and get the required key.
     *
     * @param key to specify the key to put
     * @return response containing the key
     */
    @NotNull
    public Response getValue(@NotNull final ByteBuffer key) throws IOException {
        try {
            final byte[] value = evaluateFromBuffer(key);
            return new Response(Response.OK, value);
        } catch (NoSuchElementException exc) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    /**
     * PUT exchange runner.
     *
     * @param key - key either to add a record or to modify existing one
     * @param request - HTTP request
     */
    public void upsertValue(@NotNull final ByteBuffer key,
                            @NotNull final Request request) throws IOException {
        dao.upsertValue(key, ByteBuffer.wrap(request.getBody()));
    }

    /**
     * DELETE exchange runner.
     *
     * @param key - key searched to remove specific record
     */
    public void deleteValue(@NotNull final ByteBuffer key) throws IOException {
        dao.removeValue(key);
    }

    /**
     * extracts key searched from HTTP request.
     *
     * @param req - key searched
     * @return key written into ByteBuffer instance
     */
    public static ByteBuffer getKeyFromRequest(@NotNull final Request req) {
        final String strKey = req.getParameter("id=");
        return ByteBuffer.wrap(strKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * retrieves value which is a match of key searched.
     *
     * @param key - key searched
     * @return value written into plain byte array
     */
    private byte[] evaluateFromBuffer(@NotNull final ByteBuffer key) throws IOException {
        final Value value = dao.getValue(key);
        if (value.isValueMissing()) {
            LOGGER.error(ReplicationLsm.NOT_FOUND_ERROR_LOG);
            throw new NoSuchElementException(ReplicationLsm.NOT_FOUND_ERROR_LOG);
        }
        return value.getBytesFromValue();
    }
}
