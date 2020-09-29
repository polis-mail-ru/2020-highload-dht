package ru.mail.polis.service.dmrnz;

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
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class ServiceImpl extends HttpServer implements Service {

    @NotNull
    private final DAO dao;

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(provideConfigFromPort(port));
        this.dao = dao;
    }

    @NotNull
    private static HttpServerConfig provideConfigFromPort(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;

        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }


    /**
     * status method, checks if server is working
     *
     * @return 200
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * GET method, provides value from DAO on {@param id} position.
     *
     * @param id key in dao
     * @return 200 / 400 / 404
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id) {

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        ByteBuffer result;

        try {
            result = dao.get(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        final byte[] resultByteArray = new byte[result.remaining()];
        result.get(resultByteArray);

        assert !result.hasRemaining();

        return new Response(Response.OK, resultByteArray);
    }

    /**
     * PUT method, updates value from DAO on {@param id} position.
     *
     * @param id      key
     * @param request new value
     * @return 201 / 400 / 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id,
                        @NotNull final Request request) {

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        try {
            dao.upsert(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * DELETE method, deletes value from DAO on {@param id} position.
     *
     * @param id key
     * @return 202 / 400 / 500
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id) {

        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        try {
            dao.remove(ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
