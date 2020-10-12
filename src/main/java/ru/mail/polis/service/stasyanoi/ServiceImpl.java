package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpServer;
import ru.mail.polis.service.Service;

public class ServiceImpl implements Service {

    private final HttpServer customServer;

    public ServiceImpl(final HttpServer customServer) {
        this.customServer = customServer;
    }

    @Override
    public void start() {
        customServer.start();
    }

    @Override
    public void stop() {
        customServer.stop();
    }
}
