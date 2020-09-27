package ru.mail.polis.service.jhoysbou;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
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

    public ServiceImpl(HttpServerConfig config, final DAO dao) {
        this.config = config;
        this.dao = dao;
    }

    @Override
    public void start() {
        try {
            this.httpServer = new LsmServer(config, dao);
        } catch (IOException e) {
            log.error("IOException {}", e);
        }
        this.httpServer.start();
    }

    @Override
    public void stop() {
        if (this.httpServer != null) {
            this.httpServer.stop();
        }
    }
}
