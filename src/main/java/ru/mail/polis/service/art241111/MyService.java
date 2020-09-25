package ru.mail.polis.service.art241111;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.art241111.handlers.BadRequestHandler;
import ru.mail.polis.service.art241111.handlers.EntityHandlers;
import ru.mail.polis.service.art241111.handlers.StatusHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

/**
 * Implementation of the Service interface.
 * Implemented checking for an incorrect path, sending status,
 * saving, deleting, and retrieving data from the database.
 * @author Artem Gerasimov
 */
public class MyService implements Service {
    private HttpServer server;
    @NotNull
    private final DAO dao;
    private final int port;

    /**
     * Create server and set handlers.
     * @param port - Port for creating the server.
     * @param dao - Database for saving data.
     */
    public MyService(final int port,
                     @NotNull final DAO dao) {
        this.port = port;
        this.dao = dao;
    }

    @Override
    public void start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port),0);
            this.server.start();
            setHandlers();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            this.server.stop(0);
        }
    }

    private void setHandlers(){
        new StatusHandler(this.server);
        new EntityHandlers(dao, this.server);
        new BadRequestHandler(this.server);
    }
}
