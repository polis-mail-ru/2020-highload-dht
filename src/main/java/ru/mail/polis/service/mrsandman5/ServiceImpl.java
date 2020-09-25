package ru.mail.polis.service.mrsandman5;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class ServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private static final Response ERROR = new Response(Response.BAD_REQUEST, Response.EMPTY);
    private static final Function<String, ByteBuffer> getKey = (String id) -> ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    private final DAO dao;

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(config(port));
        this.dao = dao;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id){
        log.debug("GET request hadling : {}", id);

        if (id.isEmpty()){ return ERROR; }

        final var key = getKey.apply(id);
        final ByteBuffer value;
        try {
            value = dao.get(key);
        } catch (NoSuchElementException e){
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET error : {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return Response.ok(toByteArray(value));
    }

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        if (buffer.hasRemaining()){
            return Response.EMPTY;
        }

        final var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id,
                        @NotNull final Request request){
        log.debug("PUT request handling : {} with {} byte(s)", id, request.getBody().length);

        if (id.isEmpty()){ return ERROR; }

        final var key = getKey.apply(id);
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        try {
            dao.upsert(key, value);
        } catch (IOException e) {
            log.error("PUT error : {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id){
        log.debug("DELETE request handling : {}", id);

        if (id.isEmpty()){ return ERROR; }

        final var key = getKey.apply(id);
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("DELETE error : {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Path("/v0/status")
    public Response status(){
        return Response.ok("OK");
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        log.error("Invalid request : {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @NotNull
    private static HttpServerConfig config(final int port) {
        final var acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final var config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        return config;
    }
}
