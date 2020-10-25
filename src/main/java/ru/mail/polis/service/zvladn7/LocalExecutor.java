package ru.mail.polis.service.zvladn7;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Implement this interface for processing http requests.
 */
@FunctionalInterface
public interface LocalExecutor {

    /**
     * Execute local request and return response.
     */
    @NotNull
    Response execute();

}
