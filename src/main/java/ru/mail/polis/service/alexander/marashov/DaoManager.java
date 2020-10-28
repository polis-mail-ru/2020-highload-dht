package ru.mail.polis.service.alexander.marashov;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DaoManager {

    private static final Logger log = LoggerFactory.getLogger(DaoManager.class);

    private final DAO dao;

    public DaoManager(final DAO dao) {
        this.dao = dao;
    }

    /**
     * Method that puts entity to the DAO and returns response with operation results.
     *
     * @param key   - ByteBuffer that contains the key data.
     * @param value - ByteBuffer that contains the value data.
     * @return response to send.
     */
    public Response put(final ByteBuffer key, final ByteBuffer value) {
        try {
            this.dao.upsert(key, value);
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (final IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Method that deletes entity from the DAO and returns response with operation results.
     *
     * @param key - ByteBuffer that contains the key data.
     * @return response to send.
     */
    public Response delete(final ByteBuffer key) {
        try {
            this.dao.remove(key);
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (final IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Method that gets entity from the DAO and returns response with operation results.
     *
     * @param key - ByteBuffer that contains key data.
     * @return response to send.
     */
    public Response rowGet(final ByteBuffer key) {
        Value value = null;
        try {
            value = this.dao.rowGet(key);
        } catch (final IOException e) {
            log.debug("Key not found", e);
        }

        Response response;
        if (value == null) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            try {
                final byte[] serializedData = ValueSerializer.serialize(value);
                response = new Response(Response.OK, serializedData);
            } catch (IOException e) {
                response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                log.error("Local get SERIALIZE ERROR");
            }
        }
        return response;
    }

    public void close() {
        try {
            dao.close();
        } catch (final IOException e) {
            log.error("Error closing dao", e);
        }
    }
}
