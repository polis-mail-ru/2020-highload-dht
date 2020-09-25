package ru.mail.polis.service.jhoysbou;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Path;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.io.ObjectInputFilter;

public class LsmService extends HttpServer implements Service {
//    private final DAO dao;

    public LsmService(final int port, final DAO dao) throws IOException {
        super(makeConfig(port));
//        this.dao = dao;
    }

    private static HttpServerConfig makeConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

//    @Path("/v0/status")
//    public Response status() {
//
//    }
}
