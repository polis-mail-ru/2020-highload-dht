package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpServer;

public class BadRequestHandler {
    public BadRequestHandler(final HttpServer server) {
        setBadRequestHandler(server);
    }

    private void setBadRequestHandler(final HttpServer server) {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(400,0);
            exchange.close();
        });
    }
}
