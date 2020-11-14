package ru.mail.polis.service.codearound;

import one.nio.http.Response;

import java.io.IOException;

/**
 *  implemented for async handler running in single node topology.
 */
@FunctionalInterface
public interface AsyncExec {
    Response exec() throws IOException;
}
