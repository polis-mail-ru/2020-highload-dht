package ru.mail.polis.service.ivlev;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class ServiceImpl implements Service {

    private final DAO dao;
    private final HttpServerConfig config;
    private HttpServer server;

    /**
     * Конструктор класса ServiceImpl.
     *
     * @param port - порт
     * @param dao  - реализация DAO
     */
    public ServiceImpl(final int port, @NotNull final DAO dao) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        acceptorConfig.deferAccept = true;
        this.config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        this.dao = dao;
    }

    @Override
    public void start() throws IOException {
        this.server = new ThreadController(config, dao, Runtime.getRuntime().availableProcessors(), 16);
        this.server.start();
    }

    @Override
    public void stop() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}
