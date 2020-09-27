package ru.mail.polis.service.jhoysbou;

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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class LsmServer extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(LsmServer.class);
    private final DAO dao;

    public LsmServer(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Provides an entity by the id.
     *
     * @param id - String
     *
     * @return response - Responce
     *
     * <p> Http code status:
     * 200 - successfully get value by key
     * 400 - no id or it is empty
     * 404 - no value was found for a provided key
     * 500 - internal error
     * </p>
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getValue(@Param(value = "id", required = true) final String id) {
        if ("".equals(id)) {
            log.error("Couldn't get value with empty key");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));

        ByteBuffer value = null;
        try {
            value = dao.get(idBuffer);
        } catch (IOException e) {
            log.error("Error getting {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            log.info("There is now element with key {}", id, e);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        final byte[] array = new byte[value.remaining()];
        value.get(array);
        return new Response(Response.OK, array);
    }

    /**
     * Save or update an entity by id.
     *
     * @param id      - String
     * @param request - Request with value to save in body
     *
     * @return response - Responce
     *
     * <p>Http code status:
     * 200 - value successfully saved or updated
     * 400 - no id or it is empty
     * 500 - internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putValue(@Param(value = "id", required = true) final String id,
                             final Request request) {
        if ("".equals(id)) {
            log.error("Couldn't put value with empty key");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        final ByteBuffer valueBuffer = ByteBuffer.wrap(request.getBody());

        try {
            dao.upsert(idBuffer, valueBuffer);
        } catch (IOException e) {
            log.error("Error putting element with id = {}", id, e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * Delete an entity by id.
     *
     * @param id - String
     *
     * @return response - Responce
     *
     * <p>Http code status:
     * 201 - value successfully deleted
     * 400 - no id or it is empty
     * 500 - internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteValue(@Param(value = "id", required = true) final String id) throws IOException {
        if ("".equals(id)) {
            log.error("Couldn't delete value with empty key");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer idBuffer = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        dao.remove(idBuffer);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        log.error("Unknown request: {}", request);
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

}
