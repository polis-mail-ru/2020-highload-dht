package ru.mail.polis.service.jhoysbou;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class ServiceImpl implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final HttpServerConfig config;
    private final DAO dao;
    private HttpServer httpServer;

    /**
     * Prepare an http server to be started.
     *
     * @param port – int  port to listen
     * @param dao  - DAO implementation
     * @return instance of ServiceImpl
     */
    public ServiceImpl(final int port, final DAO dao) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        this.config = new HttpServerConfig();
        this.config.acceptors = new AcceptorConfig[]{acceptorConfig};

        this.dao = dao;
    }

    @Override
    public void start() {
        try {
            this.httpServer = new LsmServer(config, dao);
            log.info("Server started successfully");
        } catch (IOException e) {
            log.error("Couldn't start the server", e);
        }
        this.httpServer.start();
    }

    @Override
    public void stop() {
        if (this.httpServer != null) {
            this.httpServer.stop();
            log.info("Server stopped");
        }
    }
}
