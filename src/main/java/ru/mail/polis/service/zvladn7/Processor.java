package ru.mail.polis.service.zvladn7;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Implement this interface for processing http requests.
 */
@FunctionalInterface
public interface Processor {

    /**
     * Process http requests.
     * @throws IOException - error sending response
     */
    CompletableFuture<Response> process(@NotNull final ReplicasHolder holder) throws IOException;

}
