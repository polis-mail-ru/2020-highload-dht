package ru.mail.polis.service.kovalkov;

import com.google.common.net.HttpHeaders;
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
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.*;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final DAO dao;

    public ServiceImpl(final HttpServerConfig config, final DAO dao, final Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    /**
     * Check status.
     *
     * @return - return code 200 OK.
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        final Response response = new Response(Response.OK);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    /**
     * Get value by key.
     *
     * @param id - key.
     * @return - value as byte array also return code 404 if not found and status 400 if key empty.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param("id") final String id) {
        if (id == null || id.isEmpty()) {
            final Response response = new Response(Response.BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return response;
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
        ByteBuffer value;
        try {
            value = dao.get(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            log.error("Method get. IO exception. ", e);
            final Response notFoundResponse = new Response(Response.NOT_FOUND);
            notFoundResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return notFoundResponse;
        }
        final byte[] bytes = BufferConverter.unfoldToBytes(value);
        return Response.ok(bytes);
    }

    /**
     * Put key and value to LSM.
     *
     * @param id - key.
     * @param request - contains value in the body.
     * @return - code 201 if all's ok, and status 400 if key empty.
     */
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
            log.error("Method put. IO exception. ", e);
            throw new RuntimeException(e);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * Delete by key.
     *
     * @param id - key.
     * @return - code 202 if all's ok, and status 400 if key empty.
     */
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
            throw new RuntimeException(e);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, new byte[0]));
    }

    @Override
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            log.error("Dao IO exception when try close: ", e);
            throw new RuntimeException(e);
        }
    }
}
