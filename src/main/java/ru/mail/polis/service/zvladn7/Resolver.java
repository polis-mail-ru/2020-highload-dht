package ru.mail.polis.service.zvladn7;

import one.nio.http.Response;

import java.io.IOException;
import java.util.List;

/**
 * Implement this interface for processing http requests.
 */
@FunctionalInterface
public interface Resolver {

    /**
     * Execute local request and return response.
     */
    void resolve(final List<Response> responses) throws IOException;

}
