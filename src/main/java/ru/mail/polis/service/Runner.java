package ru.mail.polis.service;

import one.nio.http.Response;

import java.io.IOException;

@FunctionalInterface
interface Runner {
    Response execute() throws IOException;
}
