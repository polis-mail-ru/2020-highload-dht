package ru.mail.polis.service.stasyanoi;

import ru.mail.polis.service.Service;

public class ServiceImpl implements Service {

    private final CustomServer customServer;

    public ServiceImpl(CustomServer customServer) {
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
