package ru.mail.polis.service.StasyanOi;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;

public class ServiceImpl implements Service {

    private final DAO dao;

    public ServiceImpl(DAO dao) {
        this.dao = dao;
    }

    @Override
    public void start() {
        while (true);
    }

    @Override
    public void stop() {
        try {
            dao.get(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
