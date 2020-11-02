package ru.mail.polis.service.zvladn7;

import one.nio.http.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implement this interface for processing http requests.
 */
@FunctionalInterface
public interface Resolver<T> {

    /**
     * Execute local request and return response.
     */
    CompletableFuture<Response> resolve(final int ack,
                                        final List<CompletableFuture<T>> responses) throws IOException;

}
