package ru.mail.polis.service.alexander.marashov;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DaoManager {

    private static final Logger log = LoggerFactory.getLogger(DaoManager.class);

    private final DAO dao;
    private final ExecutorService daoExecutor;

    public DaoManager(final DAO dao, final ExecutorService daoExecutor) {
        this.dao = dao;
        this.daoExecutor = daoExecutor;
    }

    /**
     * Method that puts entity to the DAO and returns response with operation results.
     *
     * @param key   - ByteBuffer that contains the key data.
     * @param value - ByteBuffer that contains the value data.
     * @return response to send.
     */
    public CompletableFuture<Response> put(final ByteBuffer key, final ByteBuffer value) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        this.dao.upsert(key, value);
                        return new Response(Response.CREATED, Response.EMPTY);
                    } catch (final IOException e) {
                        return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                    }
                }
        );
    }

    /**
     * Method that deletes entity from the DAO and returns response with operation results.
     *
     * @param key - ByteBuffer that contains the key data.
     * @return response to send.
     */
    public CompletableFuture<Response> delete(final ByteBuffer key) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        this.dao.remove(key);
                        return new Response(Response.ACCEPTED, Response.EMPTY);
                    } catch (final IOException e) {
                        return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                    }
                }
        );
    }

    /**
     * Method that gets entity from the DAO and returns response with operation results.
     *
     * @param key - ByteBuffer that contains key data.
     * @return response to send.
     */
    public CompletableFuture<Value> rowGet(final ByteBuffer key) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return this.dao.rowGet(key);
                    } catch (final NoSuchElementException e) {
                        log.debug("Key not found", e);
                        return null;
                    } catch (final IOException e) {
                        log.error("Local get: DAO error", e);
                        throw new RuntimeException(e);
                    }
                },
                daoExecutor
        );
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
