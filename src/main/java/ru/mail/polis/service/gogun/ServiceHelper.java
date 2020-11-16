package ru.mail.polis.service.gogun;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.gogun.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static ru.mail.polis.service.gogun.AsyncServiceImpl.log;

public class ServiceHelper {

    public static final String TIMESTAMP_HEADER = "timestamp: ";
    private final DAO dao;

    public ServiceHelper(@NotNull final DAO dao) {
        this.dao = dao;
    }

    Response handlePut(@NotNull final ByteBuffer key,
                       @NotNull final Request request) {
        try {
            this.dao.upsert(key, ServiceUtils.getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    Response handleGet(@NotNull final ByteBuffer key) {
        final Value value;
        Response response;
        try {
            value = this.dao.getValue(key);
        } catch (IOException e) {
            log.error("Internal server error get", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
            response.addHeader(TIMESTAMP_HEADER + AsyncServiceImpl.ABSENT);
            return response;
        }

        if (value.isTombstone()) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            response = Response.ok(ServiceUtils.getArray(value.getData()));
        }
        response.addHeader(TIMESTAMP_HEADER + value.getTimestamp());

        return response;
    }

    Response handleDel(@NotNull final ByteBuffer key) {
        try {
            this.dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

}
