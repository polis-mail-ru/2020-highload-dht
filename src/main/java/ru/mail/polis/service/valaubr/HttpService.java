package ru.mail.polis.service.valaubr;

import com.google.common.base.Charsets;
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
import java.util.NoSuchElementException;

public class HttpService extends HttpServer implements Service {

    /**
     * key-value storage.
     */
    private final DAO dao;
    /**
     * slf4j logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HttpService.class);

    /**
     * Constructor of the service.
     *
     * @param port - port to connection
     * @param base - object of storage
     * @throws IOException
     */
    public HttpService(final int port, @NotNull final DAO base) throws IOException {
        super(config(port));
        dao = base;
    }

    private static HttpServerConfig config(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    private byte[] converterFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    /**
     * Get status of server.
     *
     * @return 200 - OK
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    /**
     * Getting Entity by id.
     *
     * @param id - Entity id
     * @return 200 - OK
     * 400 - Empty id in param
     * 404 - No such element in dao
     * 500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(required = true, value = "id") @NotNull final String id) {
        if (id.strip().isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            return Response.ok(converterFromByteBuffer(dao.get(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)))));
        } catch (NoSuchElementException e) {
            logger.error("Record not exist by id = {}", id);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            logger.error("Error when getting record", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Insertion entity dao by id.
     *
     * @param id - Entity id
     * @return 201 - Create entity
     * 400 - Empty id in param
     * 500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(required = true, value = "id") @NotNull final String id, @NotNull final Request request) {
        if (id.strip().isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("Error when putting record", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Deleting entity from dao by id.
     *
     * @param id - Entity id
     * @return 202 - Delete entity
     * 400 - Empty id in param
     * 500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(required = true, value = "id") @NotNull final String id) {
        if (id.strip().isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        try {
            dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("Error when deleting record", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }
}
