package ru.mail.polis.service.bmendli;

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
import ru.mail.polis.dao.bmendli.NoSuchElementExceptionLightWeight;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MyService extends HttpServer implements Service {

    private final Logger logger = LoggerFactory.getLogger(MyService.class);

    @NotNull
    private final DAO dao;

    public MyService(final int port, @NotNull final DAO dao) throws IOException {
        super(createConfigFromPort(port));
        this.dao = dao;
    }

    /**
     * Get request. Return a data which associated with
     * passed id in path '/v0/entity' from dao.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@NotNull @Param(required = true, value = "id") final String id) {
        try {
            if (id.isBlank()) {
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            final byte[] bytes = id.getBytes(Charsets.UTF_8);
            final ByteBuffer wrappedBytes = ByteBuffer.wrap(bytes);
            final ByteBuffer byteBuffer = dao.get(wrappedBytes);
            return Response.ok(getBytesFromByteBuffer(byteBuffer));
        } catch (NoSuchElementExceptionLightWeight e) {
            logger.error("Does not exist record by id = {}", id, e);
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ioe) {
            logger.error("Error when trying get record", ioe);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Put request. Put data in dao which associated with
     * passed id in path '/v0/entity'.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@NotNull @Param(required = true, value = "id") final String id,
                        @NotNull final Request request) {
        try {
            if (id.isBlank()) {
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException ioe) {
            logger.error("Error when trying put record", ioe);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete request. Delete data from dao which associated with
     * passed id in path '/v0/entity'.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@NotNull @Param(required = true, value = "id") final String id) {
        try {
            if (id.isBlank()) {
                return new Response(Response.BAD_REQUEST, Response.EMPTY);
            }
            dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException ioe) {
            logger.error("Error when trying delete record", ioe);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Return status for path '/v0/status'.
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.EMPTY);
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @NotNull
    private static byte[] getBytesFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    @NotNull
    private static HttpServerConfig createConfigFromPort(final int port) {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.selectors = 4;
        return config;
    }
}
