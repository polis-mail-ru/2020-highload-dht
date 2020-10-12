package ru.mail.polis.service.codearound;

import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import ru.mail.polis.service.Service;

/**
 *  class which method to be invoked for initializing HTTP Server configuration.
 */
public class TaskServerConfig {

    private TaskServerConfig() {

    }

    /**
     * set HTTP server initial configuration.
     * @param port - server listening port
     * @return HTTP server configuration object
     */
    public static HttpServerConfig getConfig(final int port) {
        if (port <= 1024 || port >= 65536) {
            throw new IllegalArgumentException("Invalid port");
        }
        final AcceptorConfig acc = new AcceptorConfig();
        final HttpServerConfig config = new HttpServerConfig();
        acc.port = port;
        acc.deferAccept = true;
        acc.reusePort = true;
        config.acceptors = new AcceptorConfig[]{acc};
        return config;
    }
}
