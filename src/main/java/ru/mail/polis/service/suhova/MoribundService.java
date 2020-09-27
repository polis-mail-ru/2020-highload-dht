package ru.mail.polis.service.suhova;

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

public class MoribundService extends HttpServer implements Service {

    @NotNull
    private final DAO dao;
    private static final Logger logger = LoggerFactory.getLogger(MoribundService.class);

    public MoribundService(final int port, @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    /**
     * Request to get a value by id
     * Path /v0/entity
     *
     * @param id - key
     * @return 200 OK + value/ 400 / 404 / 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id) {
        try {
            if (id.isEmpty()) {
                logger.debug("FAIL GET! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            return new Response(Response.OK, toByteArray(dao.get(toByteBuffer(id))));
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            logger.debug("FAIL GET! id: {}, error: {}", id, e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Request to put a value by id
     * Path /v0/entity
     *
     * @param id - key
     * @return 201 Created/ 400 / 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id, final Request request) {
        try {
            if (id.isEmpty()) {
                logger.debug("FAIL PUT! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            logger.debug("FAIL PUT! id: {}, request: {}, error: {}", id, request.getBody(), e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Request to delete a value by id
     * Path /v0/entity
     *
     * @param id - key
     * @return 202 Accepted/ 400 Bad request/ 500 Error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id) {
        try {
            if (id.isEmpty()) {
                logger.debug("FAIL DELETE! Id is empty!");
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.remove(toByteBuffer(id));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            logger.debug("FAIL DELETE! id: {}, error: {}", id, e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * All requests to /v0/status
     *
     * @return 200 OK
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private static HttpServerConfig getConfig(final int port) {
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.reusePort = true;
        HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    private static ByteBuffer toByteBuffer(@NotNull final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] toByteArray(@NotNull final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }
        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }
}
