package ru.mail.polis.service.art241111.handlers;

import com.google.common.base.Charsets;
import one.nio.http.Request;
import one.nio.http.Response;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class EntityHandlers {
    private final ByteBuffer key;
    private final DAO dao;

    public EntityHandlers(final ByteBuffer key, final DAO dao) {
        this.key = key;
        this.dao = dao;
    }

    /**
     * Get Setting the response to receiving the GET command.
     * @return The reaction of the server.
     */
    public Response setGetHandler() {
        try {
            final ByteBuffer value = dao.get(key);
            final ByteBuffer duplicate = value.duplicate();
            final byte[] body = new byte[duplicate.remaining()];
            duplicate.get(body);

            return new Response(Response.OK, body);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, "Key not founded".getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Get Setting the response to receiving the PuT command.
     * @return The reaction of the server.
     */
    public Response setPutHandler(final Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Get Setting the response to receiving the GET command.
     * @return The reaction of the server.
     */
    public Response setDeleteHandler() {
        try {
            dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
