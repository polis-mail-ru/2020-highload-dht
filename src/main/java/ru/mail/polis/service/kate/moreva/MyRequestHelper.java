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
import java.util.List;
import java.util.NoSuchElementException;

public class MyRequestHelper {
    private static final String SERVER_ERROR = "Server error can't send response";
    private static final String TIMESTAMP = "Timestamp: ";
    private static final String TOMBSTONE = "Tombstone: ";
    private static final String PROXY_HEADER = "X-Proxy-For:";
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
        final Cell cell;
        try {
            cell = dao.getCell(key);
            final Response response;
            if (cell.getValue().isTombstone()) {
                response = new Response(Response.OK, Response.EMPTY);
            } else {
                final ByteBuffer value = dao.get(key);
                final byte[] body = new byte[value.remaining()];
                value.get(body);
                response = new Response(Response.OK, body);
            }
            response.addHeader(TIMESTAMP + cell.getValue().getTimestamp());
            response.addHeader(TOMBSTONE + cell.getValue().isTombstone());
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
    public Response mergeResponses(final List<Response> responseList) {
        Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
        long time = Long.MIN_VALUE;
        int correctResponses = 0;
        int missedResponses = 0;
        for (final Response resp : responseList) {
            final int status = resp.getStatus();
            if (status == 200) {
                correctResponses++;
                if (getTimestamp(resp) > time) {
                    time = getTimestamp(resp);
                    response = resp;
                }
            } else if (status == 404) {
                missedResponses++;
                correctResponses++;
            }
        }
        final boolean tomb = Boolean.parseBoolean(response.getHeader(TOMBSTONE));
        if (tomb || correctResponses == missedResponses) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return response;
    }

    /**
     * Returns response status.
     */
    public int getStatus(final Response response) {
        return response.getStatus();
    }

    /**
     * Checks whether the request is proxied.
     */
    public boolean isProxied(final Request request) {
        return request.getHeader(PROXY_HEADER) != null;
    }

    /**
     * Returns response timestamp.
     */
    public static long getTimestamp(final Response response) {
        final String timestamp = response.getHeader(TIMESTAMP);
        return timestamp == null ? -1 : Long.parseLong(timestamp);
    }

    /**
     * Creates request according to replication status.
     * {@code 504} (not enough replicas).
     * {@param response} (enough replicas).
     */
    public void correctReplication(final int ack,
                                   final Replicas replicas,
                                   final HttpSession session,
                                   final String response) {
        if (ack < replicas.getAck()) {
            sendLoggedResponse(session, new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY));
        } else {
            sendLoggedResponse(session, new Response(response, Response.EMPTY));
        }
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
