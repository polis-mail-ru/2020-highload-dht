package ru.mail.polis.service.art241111.handlers;

import com.google.common.base.Charsets;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.art241111.MyHttpServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public final class EntityHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyHttpServer.class);

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final DAO dao;

    public EntityHandler(@NotNull final ByteBuffer key, @NotNull final DAO dao) {
        this.key = key;
        this.dao = dao;
    }

    /**
     * Get Setting the response to receiving the GET command.
     * @return The reaction of the server.
     */
    public Response handleGetRequest() {
        try {
            final ByteBuffer value = dao.get(key);
            final byte[] body = new byte[value.remaining()];
            value.get(body);

            logger.debug("Handler GET {} {}", key, body);
            return Response.ok(body);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, "Key not founded".getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            logger.error("IOException in getting {} from dao", key);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Get Setting the response to receiving the PuT command.
     * @return The reaction of the server.
     */
    public Response handlePutRequest(final Request request) {
        try {
            dao.upsert(key, ByteBuffer.wrap(request.getBody()));

            logger.debug("Handler PUT {}", key);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("IOException in putting {} from dao", key);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Get Setting the response to receiving the GET command.
     * @return The reaction of the server.
     */
    public Response handleDeleteRequest() {
        try {
            dao.remove(key);

            logger.debug("Handler DELETE {}", key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("IOException in deleting {} from dao", key);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }
}
