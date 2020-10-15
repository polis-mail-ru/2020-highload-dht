package ru.mail.polis.service.ivlev;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class ServiceImpl implements Service {

    private final HttpServer server;

    /**
     * Конструктор класса ServiceImpl.
     *
     * @param port - порт
     * @param dao  - реализация DAO
     */
    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        acceptorConfig.deferAccept = true;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        this.server = new ThreadController(config, dao, Runtime.getRuntime().availableProcessors(), 1024);
    }

    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}
