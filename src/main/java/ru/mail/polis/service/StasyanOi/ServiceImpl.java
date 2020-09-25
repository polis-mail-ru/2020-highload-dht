package ru.mail.polis.service.StasyanOi;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

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
