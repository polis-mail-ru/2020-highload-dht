package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
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
import java.util.logging.Level;

public final class BasicService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(BasicService.class);
    private static final byte[] EMPTY = Response.EMPTY;
    private final DAO dao;
    
    private BasicService(final int port, @NotNull final DAO dao) throws IOException {
        super(configFrom(port));
        this.dao = dao;
    }
    
    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        
        final HttpServerConfig config = new HttpServerConfig();
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
    public static BasicService of(final int port, @NotNull final DAO dao) throws IOException {
        return new BasicService(port, dao);
    }
    
    @Path("/v0/status")
    public Response handleSimple() throws IOException {
        return Response.ok("OK");
    }
    
    /**
     * Basic implementation of http get handling.
     *
     * @param id key in database
     * @return response with value from database by key or http error code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response handleGet(@Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            logger.error("Empty key in getting");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            final ByteBuffer value = dao.get(buffer);
            return Response.ok(fromByteBuffer(value));
        } catch (NoSuchElementException e) {
            logger.error("No such element key(size: {}) in dao", id.length());
            return new Response(Response.NOT_FOUND, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in getting key(size: {}) from dao", id.length());
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    /**
     * Basic implementation of http put handling.
     *
     * @param id      key
     * @param request value for putting in database
     * @return response with http code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response handlePut(@Param(value = "id", required = true) final String id, final Request request) {
        if (id.isEmpty()) {
//            logger.error("Empty key in putting");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            dao.upsert(buffer, ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, EMPTY);
        } catch (IOException e) {
//            logger.error("IOException in putting key(size: {}) from dao", id.length());
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    /**
     * Basic implementation of http put handling.
     *
     * @param id key in database to delete
     * @return response with http code
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response handleDelete(@Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            logger.error("Empty key in deleting");
            return new Response(Response.BAD_REQUEST, EMPTY);
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            dao.remove(buffer);
            return new Response(Response.ACCEPTED, EMPTY);
        } catch (IOException e) {
            logger.error("IOException in removing key(size: {}) from dao", id.length());
            return new Response(Response.INTERNAL_ERROR, EMPTY);
        }
    }
    
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        logger.error("Unhandled request: {}", request);
        session.sendResponse(new Response(Response.BAD_REQUEST, EMPTY));
    }
}
