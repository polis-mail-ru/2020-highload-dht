package ru.mail.polis.service.ivlev;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ServiceController extends HttpServer {

    private final DAO dao;

    public ServiceController(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config, dao);
        this.dao = dao;
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response getStatus() {
        return Response.ok("OK");
    }

    /**
     * Контроллер на end-point "/v0/entity". Работаюет с методами get, put, delete.
     *
     * @param id      - id
     * @param request - запрос
     * @return response - ответ
     */
    @Path("/v0/entity")
    public Response entity(@Param(value = "id", required = true) final String id, @NotNull final Request request) {
        if (Objects.isNull(id) || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(Charset.defaultCharset()));

        return getResponse(key, request);
    }

    private Response getResponse(final ByteBuffer key, @NotNull final Request request) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET: {
                    return methodGet(key);
                }
                case Request.METHOD_PUT: {
                    return methodPut(key, request);
                }
                case Request.METHOD_DELETE: {
                    return methodDelete(key);
                }
                default:
                    return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
            }
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response methodGet(final ByteBuffer key) {
        try {
            final ByteBuffer record = dao.get(key);
            final byte[] body = new byte[record.remaining()];
            record.get(body);
            return new Response(Response.OK, body);
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private Response methodPut(final ByteBuffer key, @NotNull final Request request) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(request.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response methodDelete(final ByteBuffer key) throws IOException {
        dao.remove(key);
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, @NotNull final HttpSession session) throws IOException {
        final Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }
}
