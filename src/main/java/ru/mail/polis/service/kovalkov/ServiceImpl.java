package ru.mail.polis.service.kovalkov;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.Utils.BufferConverter;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final DAO dao;


    public ServiceImpl(HttpServerConfig config, DAO dao, Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Path("/v0/status")
    public Response status() {
        return new Response(Response.OK);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param("id") final String id) {
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer value;
        try {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
             value = dao.get(key);
        }catch (NoSuchElementException e){
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error("Method get. IO exception. ", e);
            throw new RuntimeException("Method GET. IO exception occurred");
        }
        final byte[] bytes = BufferConverter.unfoldToBytes(value);
        return Response.ok(bytes);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param("id") final String id, final Request request) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        try {
            dao.upsert(key,value);
        } catch (IOException e) {
            log.error("Method get. IO exception. ", e);
            throw new RuntimeException("Method GET. IO exception occurred");
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param("id") final String id) {
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Method get. IO exception. ", e);
            throw new RuntimeException("Method GET. IO exception occurred");
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, new byte[0]));
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            log.error("Dao IO exception when try close: ", e);
            throw new RuntimeException("Dao IO exception when try close");
        }
    }
}
