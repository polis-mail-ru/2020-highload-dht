package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConstantsServer extends HttpServer {

    protected static final String TRUE_VAL = "true";
    protected static final String REPS = "reps";
    protected static final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");

    public ConstantsServer(HttpServerConfig config) throws IOException {
        super(config);
    }

}
