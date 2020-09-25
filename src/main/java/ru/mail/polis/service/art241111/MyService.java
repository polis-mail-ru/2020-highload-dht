package ru.mail.polis.service.art241111;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.art241111.codes.CommandsCode;
import ru.mail.polis.service.art241111.handlers.BadRequestHandler;
import ru.mail.polis.service.art241111.handlers.EntityHandlers;
import ru.mail.polis.service.art241111.handlers.StatusHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyService implements Service {
    @NotNull
    private final HttpServer server;

    public MyService(int port,
                     @NotNull final DAO dao) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port),0);

        new StatusHandler(this.server);
        new EntityHandlers(dao, this.server);
        new BadRequestHandler(this.server);
    }

    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        this.server.stop(0);
    }
}
