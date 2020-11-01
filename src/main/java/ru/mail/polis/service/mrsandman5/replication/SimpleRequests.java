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
    public Entry get(@NotNull final ByteBuffer key) {
        try {
            return Entry.entryFromBytes(key, dao);
        } catch (NoSuchElementException | IOException e) {
            return null;
        }
    }

    /**
     * Put value.
     * {@code 201} (new value created).
     * {@code 500} (internal server error occurred).
     */
    public void put(@NotNull final ByteBuffer key,
                         @NotNull final byte[] bytes) {
        try {
            final ByteBuffer body = ByteBuffer.wrap(bytes);
            dao.upsert(key, body);
        } catch (IOException ignored) {
        }
    }

    /**
     * Delete value by the key.
     * {@code 202} (value deleted).
     * {@code 500} (internal server error occurred).
     */
    public void delete(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException ignored) {
        }
    }

}
