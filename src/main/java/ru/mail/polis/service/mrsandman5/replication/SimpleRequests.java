package ru.mail.polis.service.mrsandman5.replication;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class SimpleRequests {

    private static final Logger log = LoggerFactory.getLogger(SimpleRequests.class);

    private final DAOImpl dao;
    private final ExecutorService executor;

    public SimpleRequests(@NotNull final DAOImpl dao,
                          @NotNull final ExecutorService executor) {
        this.dao = dao;
        this.executor = executor;
    }

    /**
     * Get value response.
     * {@code 200, value} (data is found).
     * {@code 404} (data is not found).
     * {@code 500} (internal server error occurred).
     */
    public CompletableFuture<Response> get(@NotNull final ByteBuffer key) {
        log.info("GET");
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final Entry value = Entry.entryFromBytes(key, dao);
                        log.info("GET value: {}", Entry.entryToResponse(value));
                        return Entry.entryToResponse(value);
                    } catch (NoSuchElementException e) {
                        return ResponseUtils.emptyResponse(Response.NOT_FOUND);
                    } catch (IOException e) {
                        return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
                    }
                }, executor);
    }

    /**
     * Get value.
     */
    public CompletableFuture<Entry> getEntry(@NotNull final ByteBuffer key) {
        log.info("GET entry");
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final Entry entry = Entry.entryFromBytes(key, dao);
                        log.info("GET entry: {}, {}, {}, {}", entry.getTimestamp(),
                                entry.getData(),
                                entry.getState(),
                                entry.getExpires());
                        return entry;
                    } catch (IOException e) {
                        throw new RuntimeException("Get future error", e);
                    }
                }, executor);
    }

    /**
     * Put value response.
     * {@code 201} (new value created).
     * {@code 500} (internal server error occurred).
     */
    public CompletableFuture<Response> put(@NotNull final ByteBuffer key,
                                           @NotNull final byte[] bytes,
                                           @NotNull final Instant expire) {
        log.info("PUT");
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final ByteBuffer body = ByteBuffer.wrap(bytes);
                        log.info("PUT entry: {}, {}, {}", key, body, expire);
                        dao.upsert(key, body, expire);
                        return ResponseUtils.emptyResponse(Response.CREATED);
                    } catch (IOException e) {
                        return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
                    }
                }, executor);
    }

    /**
     * Delete value by the key response.
     * {@code 202} (value deleted).
     * {@code 500} (internal server error occurred).
     */
    public CompletableFuture<Response> delete(@NotNull final ByteBuffer key) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        dao.remove(key);
                        return ResponseUtils.emptyResponse(Response.ACCEPTED);
                    } catch (IOException e) {
                        return ResponseUtils.emptyResponse(Response.INTERNAL_ERROR);
                    }
                }, executor);
    }

}
