package ru.mail.polis.service.art241111;

import com.google.common.base.Charsets;
import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * Simple {@link Service} implementation
 */
public class MyHttpServer extends HttpServer implements Service {
    private final DAO dao;

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    public MyHttpServer(final int port,
                        @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        super.handleDefault(request, session);
    }

    @Path("/v0/entity")
    public Response entity(
            @Param("id") final String id,
            final Request request) {
        if(id == null){
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        if("".equals(id)){
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charsets.UTF_8));
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                try {
                    ByteBuffer value = dao.get(key);
                    ByteBuffer duplicate =  value.duplicate();
                    byte[] body = new byte[duplicate.remaining()];
                    duplicate.get(body);

                    return new Response(Response.OK, body);
                } catch (NoSuchElementException e){
                    return new Response(Response.NOT_FOUND, "Key not founded".getBytes(Charsets.UTF_8));
                } catch (IOException e) {
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                }
            case Request.METHOD_PUT:
                try {
                    dao.upsert(key, ByteBuffer.wrap(request.getBody()));
                    return new Response(Response.CREATED, Response.EMPTY);
                } catch (IOException e) {
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                }
            case Request.METHOD_DELETE:
                try {
                    dao.remove(key);
                    return new Response(Response.ACCEPTED, Response.EMPTY);
                } catch (IOException e) {
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                }
            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    @Path("/v0/status")
    public Response status(
            final Request request) {
        return new Response(Response.OK, Response.EMPTY);
    }

    private static HttpServerConfig getConfig(final int port){
        if(port <= 1024 || port >= 65535){
            throw new IllegalArgumentException("Invalid port");
        }
        HttpServerConfig config = new HttpServerConfig();
        AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }
}
