package ru.mail.polis.service.s3ponia;

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

public class BasicService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(BasicService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private final DAO dao;
    
    private BasicService(final int port, @NotNull DAO dao) throws IOException {
        super(configFrom(port));
        this.dao = dao;
    }
    
    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[1];
        config.acceptors[0] = ac;
        return config;
    }
    
    @NotNull
    private static byte[] fromByteBuffer(@NotNull final ByteBuffer b) {
        final byte[] out = new byte[b.remaining()];
        b.get(out);
        return out;
    }
    
    @NotNull
    public static BasicService of(final int port, @NotNull DAO dao) throws IOException {
        return new BasicService(port, dao);
    }
    
    @Path("/v0/status")
    public Response handleSimple() throws IOException {
        return Response.ok("OK");
    }
    
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response handleGet(@Param(value = "id", required = true) final String id) {
        logger.debug("Handling getting {}...", id);
        
        if (id.isEmpty()) {
            logger.error("Empty key");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            final ByteBuffer value = dao.get(buffer);
            return Response.ok(fromByteBuffer(value));
        } catch (NoSuchElementException e) {
            logger.error("No such element {} in dao", id);
            return new Response(Response.NOT_FOUND, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in getting {} from dao", id);
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response handlePut(@Param(value = "id", required = true) final String id, final Request request) {
        logger.debug("Handling putting {}...", id);
    
        if (id.isEmpty()) {
            logger.error("Empty key");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            dao.upsert(buffer, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in putting {} from dao", id);
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response handleDelete(@Param(value = "id", required = true) final String id) {
        logger.debug("Handling removing {}...", id);
    
        if (id.isEmpty()) {
            logger.error("Empty key");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
    
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            dao.remove(buffer);
            return new Response(Response.ACCEPTED, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in removing {} from dao", id);
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }
}
