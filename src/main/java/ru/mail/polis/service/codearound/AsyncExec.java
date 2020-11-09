package ru.mail.polis.service.codearound;

import one.nio.http.Response;

import java.io.IOException;

@FunctionalInterface
public interface AsyncExec {
    Response exec() throws IOException;
}
