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

/**
 * A persistent storage with HTTP API.
 * <p>
 * The following HTTP protocol is supported:
 * <ul>
 * <li>{@code GET /v0/status} -- returns {@code 200} or {@code 503}</li>
 * <li>{@code GET /v0/entity?id=<ID>} -- get data by {@code ID}. Returns {@code 200} and data if found, {@code 404} if not found.</li>
 * <li>{@code PUT /v0/entity?id=<ID>} -- upsert (create or replace) data by {@code ID}. Returns {@code 201}.</li>
 * <li>{@code DELETE /v0/entity?id=<ID>} -- remove data by {@code ID}. Returns {@code 202}.</li>
 * </ul>
 * <p>
 * {@code ID} is a non empty char sequence.
 * <p>
 * In all the cases the storage may return:
 * <ul>
 * <li>{@code 4xx} for malformed requests</li>
 * <li>{@code 5xx} for internal errors</li>
 * </ul>
 *
 * @author Vadim Tsesko
 */
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

            var key = ByteBuffer.wrap(id.getBytes(Charset.defaultCharset()));
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
            final ByteBuffer value = dao.get(key);
            final ByteBuffer duplicate = value.duplicate();
            final byte[] body = new byte[duplicate.remaining()];
            duplicate.get(body);
            return new Response(Response.OK, body);
        } catch (NoSuchElementException | IOException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
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

    private static HttpServerConfig getConfig(final int port) {
        int PORT_MIN = 1024;
        int PORT_MAX = 65536;
        if (port <= PORT_MIN || PORT_MAX <= port) {
            throw new IllegalArgumentException(
                String.format("Invalid port value provided. It must be between %d and %d", PORT_MIN, PORT_MAX)
            );
        }

        AcceptorConfig acceptor_config = new AcceptorConfig();
        acceptor_config.port = port;
        acceptor_config.deferAccept = true;
        acceptor_config.reusePort = true;
        final HttpServerConfig server_config = new HttpServerConfig();
        server_config.acceptors = new AcceptorConfig[]{acceptor_config};
        return server_config;
    }
}
