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

    public ServiceImpl(final int port, @NotNull final DAO dao) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        this.dao = dao;
        this.config = new HttpServerConfig();
        this.config.acceptors = new AcceptorConfig[]{acceptorConfig};
        acceptorConfig.port = port;
    }

    @Override
    public void start() throws IOException {
        this.server = new ServiceController(config, dao);
        this.server.start();

    }

    @Override
    public void stop() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}
