package ru.mail.polis.service;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

import static ru.mail.polis.util.Util.toByteArray;

public class ServiceImpl extends HttpServer implements Service {
    private final DAO dao;

    ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }

    /**
     * Standard response for successful HTTP requests.
     *
     * @return HTTP status code 200 (OK)
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Provide access to entities.
     *
     * @param id key of entity
     * @param request HTTP request
     * @return response or error
     */
    @Path("/v0/entity")
    public Response entity(
            @Param("id") final String id,
            @NotNull final Request request
    ) {
        try {
            if (id == null || id.isEmpty()) {
                return new Response(
                    Response.BAD_REQUEST, "ID can't be null".getBytes(Charset.defaultCharset())
                );
            }

            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charset.defaultCharset()));
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    return get(key);
                }
                case Request.METHOD_PUT: {
                    return put(key, request);
                }
                case Request.METHOD_DELETE: {
                    return delete(key);
                }
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response get(final ByteBuffer key) {
        try {
            final ByteBuffer duplicate = dao.get(key).duplicate();
            return Response.ok(toByteArray(duplicate));
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response put(final ByteBuffer key, final Request request) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(request.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response delete(final ByteBuffer key) throws IOException {
        dao.remove(key);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    static HttpServerConfig getConfig(final int port) {
        final int portMin = 1024;
        final int portMax = 65536;
        if (port <= portMin || portMax <= port) {
            throw new IllegalArgumentException(
                String.format("Invalid port value provided. It must be between %d and %d", portMin, portMax)
            );
        }

        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig serverConfig = new HttpServerConfig();
        serverConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return serverConfig;
    }
}
