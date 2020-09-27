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

    @Path("/v0/entity")
    public Response response(@Param(value = "id", required = true) final String id,
                             final Request request) {
        log.debug("Request handling : {}", id);
        if (id.isEmpty()){ return ERROR; }

        final var key = getKey.apply(id);
        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return get(key);
            case Request.METHOD_PUT:
                return put(key, request.getBody());
            case Request.METHOD_DELETE:
                return delete(key);
            default:
                log.warn("Non-supported request : {}", id);
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    private Response get(@NotNull final ByteBuffer key) {
        final ByteBuffer value;
        try {
            value = dao.get(key);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("GET error : {}", toByteArray(key));
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return Response.ok(toByteArray(value));
    }

    private Response put(@NotNull final ByteBuffer key,
                          final byte[] body) {
        final ByteBuffer value = ByteBuffer.wrap(body);
        try {
            dao.upsert(key, value);
        } catch (IOException e) {
            log.error("PUT error : {} with value {}", toByteArray(key), toByteArray(value));
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response delete(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("DELETE error : {}", toByteArray(key));
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

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()){
            return Response.EMPTY;
        }

        final var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
