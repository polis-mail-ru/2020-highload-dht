package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kate.moreva.Cell;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRequestHelper {
    private static final String SERVER_ERROR = "Server error can't send response";
    private static final String TIMESTAMP = "Timestamp: ";
    static final String PROXY_HEADER = "X-Proxy: ";
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private static final Logger log = LoggerFactory.getLogger(MyRequestHelper.class);

    private final DAO dao;

    public MyRequestHelper(final DAO dao) {
        this.dao = dao;
    }

    /**
     * Subsidiary method to get value.
     * {@code 200, data} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    public Response getEntity(final ByteBuffer key) {
        try {
            final Cell cell = dao.getCell(key);
            final Response response;
            if (cell.getValue().isTombstone()) {
                response = new Response(Response.NOT_FOUND, Response.EMPTY);
            } else {
                final ByteBuffer value = dao.get(key);
                final byte[] body = new byte[value.remaining()];
                value.get(body);
                response = new Response(Response.OK, body);
            }
            response.addHeader(TIMESTAMP + cell.getValue().getTimestamp());
            return response;
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET method failed on /v0/entity for id {}", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Subsidiary method to put new value.
     * {@code 201} (new data created).
     * {@code 500} (internal server error occurred).
     */
    public Response putEntity(final ByteBuffer key, final Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            log.error("PUT method failed on /v0/entity for id {}, request body {}.", key.get(), request.getBody(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Subsidiary method to delete value by the key.
     * {@code 202} (data deleted).
     * {@code 500} (internal server error occurred).
     */
    public Response deleteEntity(final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            log.error("DELETE method failed on /v0/entity for id {}.", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Merges responses for GET request.
     */
    public CompletableFuture<RequestValue> merge(final CompletableFuture<List<RequestValue>> future) {
        final CompletableFuture<RequestValue> result = new CompletableFuture<>();
        future.whenCompleteAsync((r, t) -> {
            if (t != null) {
                result.complete(new RequestValue(NOT_ENOUGH_REPLICAS, Response.EMPTY, -1));
                return;
            }
            RequestValue response = new RequestValue(Response.NOT_FOUND, Response.EMPTY, -1);
            long time = Long.MIN_VALUE;
            for (final RequestValue resp : r) {
                if (resp.getTimestamp() > time) {
                    time = resp.getTimestamp();
                    response = resp;
                }
            }
            result.complete(response);
        }).exceptionally(e -> {
            log.error("Error while merge ", e);
            return null;
        });
        return result;
    }

    public CompletableFuture<List<RequestValue>> collect(final List<CompletableFuture<RequestValue>> values,
                                                         final int ack) {
        final AtomicInteger numberOfErrors = new AtomicInteger(-1);
        final List<RequestValue> results = new ArrayList<>();
        final CompletableFuture<List<RequestValue>> resultsFuture = new CompletableFuture<>();

        for (final CompletableFuture<RequestValue> value : values) {
            value.whenComplete((v, t) -> {
                if (t != null) {
                    if (numberOfErrors.incrementAndGet() == (values.size() - ack)) {
                        resultsFuture.completeExceptionally(new RejectedExecutionException(t));
                    }
                    return;
                }
                if (results.size() <= ack) {
                    results.add(v);
                    if (results.size() == ack) {
                        resultsFuture.complete(results);
                    }
                }
            }).exceptionally(e -> {
                log.error("Error while collecting futures ", e);
                return null;
            });
        }
        return resultsFuture;
    }

    public String parseStatusCode(final int status) {
        switch (status) {
            case 200:
                return Response.OK;
            case 201:
                return Response.CREATED;
            case 202:
                return Response.ACCEPTED;
            case 400:
                return Response.BAD_REQUEST;
            case 404:
                return Response.NOT_FOUND;
            case 500:
                return Response.INTERNAL_ERROR;
            case 504:
                return NOT_ENOUGH_REPLICAS;
            default:
                throw new UnsupportedOperationException(status + "not available");
        }
    }

    /**
     * Checks whether the request is proxied.
     */
    public boolean isProxied(final Request request) {
        return request.getHeader(PROXY_HEADER) != null;
    }

    public long getTimestamp(final Response response) {
        final String timestamp = response.getHeader(TIMESTAMP);
        return timestamp == null ? -1 : Long.parseLong(timestamp);
    }

    /**
     * Sends response.
     */
    public void sendLoggedResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }
}
