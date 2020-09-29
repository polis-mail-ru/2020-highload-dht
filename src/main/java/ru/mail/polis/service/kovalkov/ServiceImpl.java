package ru.mail.polis.service.kovalkov;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final DAO dao;


    public ServiceImpl(HttpServerConfig config, DAO dao, Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            log.error("Dao IO exception when try close: ", e);
            throw new RuntimeException("Dao IO exception when try close");
        }
    }
}
