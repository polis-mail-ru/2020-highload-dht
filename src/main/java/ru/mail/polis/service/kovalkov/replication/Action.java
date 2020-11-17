package ru.mail.polis.service.kovalkov.replication;

import one.nio.http.Response;

import java.io.IOException;

@FunctionalInterface
public interface Action {

    Response act() throws IOException;
}
