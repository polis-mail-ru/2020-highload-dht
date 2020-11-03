package ru.mail.polis.service.alexander.marashov;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

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
        final Value value;
        try {
            value = this.dao.rowGet(key);
        } catch (final IOException | NoSuchElementException e) {
            log.debug("Key not found", e);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        try {
            final byte[] serializedData = ValueSerializer.serialize(value);
            return new Response(Response.OK, serializedData);
        } catch (IOException e) {
            log.error("Local get serialize error");
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Closes the DAO or writes an error to the log.
     */
    public void close() {
        try {
            dao.close();
        } catch (final IOException e) {
            throw new RuntimeException("Error closing dao", e);
        }
    }
}
