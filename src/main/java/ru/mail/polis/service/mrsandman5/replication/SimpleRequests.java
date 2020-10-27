package ru.mail.polis.service.mrsandman5.replication;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public final class SimpleRequests {

    private final DAOImpl dao;

    public SimpleRequests(@NotNull final DAOImpl dao) {
        this.dao = dao;
    }

    /**
     * Get value.
     * {@code 200, value} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    public Response get(@NotNull final ByteBuffer key) {
        try {
            final Entry value = Entry.entryFromBytes(key, dao);
            return Entry.entryToResponse(value);
        } catch (NoSuchElementException e) {
            return ResponseUtils.emptyResponse(Response.NOT_FOUND);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    /**
     * Put value.
     * {@code 201} (new value created).
     * {@code 500} (internal server error occurred).
     */
    public Response put(@NotNull final ByteBuffer key,
                         @NotNull final byte[] bytes) {
        try {
            final ByteBuffer body = ByteBuffer.wrap(bytes);
            dao.upsert(key, body);
            return ResponseUtils.emptyResponse(Response.CREATED);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

    /**
     * Delete value by the key.
     * {@code 202} (value deleted).
     * {@code 500} (internal server error occurred).
     */
    public Response delete(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
            return ResponseUtils.emptyResponse(Response.ACCEPTED);
        } catch (IOException e) {
            return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
        }
    }

}
