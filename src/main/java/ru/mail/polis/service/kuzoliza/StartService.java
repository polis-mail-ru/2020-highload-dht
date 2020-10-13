package ru.mail.polis.service.kuzoliza;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class StartService implements Service {

    private final HttpServerConfig config;
    private final DAO dao;
    private HttpServer server;
    private static final Logger log = LoggerFactory.getLogger(StartService.class.getName());
    private final int availableProcessors;
    private final int queueSize;

    /**
     * Service configuration.
     *
     * @param port - which port should be listened
     * @param dao - database
     */
    public StartService(final int port, final DAO dao, final int availableProcessors, final int queueSize) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;

        this.dao = dao;
        this.config = new HttpServerConfig();
        this.config.acceptors = new AcceptorConfig[]{acceptorConfig};
        this.availableProcessors = availableProcessors;
        this.queueSize = queueSize;
    }

    @Override
    public void start() {
        try {
            this.server = new MyService(config, dao, availableProcessors, queueSize);
            this.server.start();
        } catch (IOException e) {
            log.error("Can't start server");
        }
    }

    @Override
    public void stop() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}
