package ru.mail.polis.service.nik27090;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.nik27090.Cell;
import ru.mail.polis.dao.nik27090.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class DaoHelper {
    private static final Logger log = LoggerFactory.getLogger(DaoHelper.class);
    private static final String PROXY_HEADER = "Timestamp:";

    @NotNull
    private final DAO dao;

    public DaoHelper(@NotNull final DAO dao) {
         this.dao = dao;
    }

    public Response resolveGet(@NotNull final List<Response> result) {
        final Map<Response, Integer> responses = new HashMap<>();
        result.forEach(resp -> {
            final Integer val = responses.get(resp);
            responses.put(resp, val == null ? 0 : val + 1);
        });
        Response finalResult = null;
        int maxCount = -1;
        long time = Long.MIN_VALUE;
        for (final Map.Entry<Response, Integer> entry : responses.entrySet()) {
            if (entry.getValue() >= maxCount && getTimestamp(entry.getKey()) > time) {
                time = getTimestamp(entry.getKey());
                maxCount = entry.getValue();
                finalResult = entry.getKey();
            }
        }
        return finalResult;
    }

    public static long getTimestamp(final Response response) {
        final String timestamp = response.getHeader(PROXY_HEADER);
        return timestamp == null ? -1L : Long.parseLong(timestamp);
    }

    public Response getEntity(final ByteBuffer key) {
        final Cell cell;
        try {
            cell = dao.getCell(key);
            final Value cellValue = cell.getValue();
            if (cellValue.isTombstone()) {
                final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                response.addHeader(PROXY_HEADER + cellValue.getTimestamp());
                return response;
            }
            final ByteBuffer value = dao.get(key).duplicate();
            final byte[] body = new byte[value.remaining()];
            value.get(body);
            final Response response = new Response(Response.OK, body);
            response.addHeader(PROXY_HEADER + cell.getValue().getTimestamp());
            return response;
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET method failed on /v0/entity for id {}", key.get(), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    public Response delEntity(final ByteBuffer key) {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            log.error("DELETE Internal error with key = {}", key, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    public Response putEntity(final ByteBuffer key, final ByteBuffer value) {
        try {
            dao.upsert(key, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            log.error("PUT Internal error.", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
