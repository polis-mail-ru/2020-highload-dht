package ru.mail.polis.service.StasyanOi;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Path;
import one.nio.http.Response;
import ru.mail.polis.dao.DAO;

import java.io.IOException;

public class CustomServer extends HttpServer {

    private final DAO dao;

    public CustomServer(DAO dao, HttpServerConfig config, Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Path("/test")
    public Response test(){
        System.out.println("test");
        return Response.ok("lol");
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
