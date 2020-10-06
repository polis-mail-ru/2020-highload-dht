package ru.mail.polis.service.ivanovandrey;

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
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {

    @NotNull
    private final DAO dao;

    public ServiceImpl(final int port,
                       @NotNull final DAO dao) throws IOException {
        super(createConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    /** Get data by key method.
     * @param id - id request.
     * @return code 200 and data,
     *         code 400 - id is empty,
     *         code 404 - data not found,
     *         code 500 - internal error
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id) {
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        final ByteBuffer val;
        try {
             val = dao.get(key);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        return Response.ok(fromByteBufferToByteArray(val));
    }

    /** Put/update data by key method.
     * @param id - id request.
     * @param request - data.
     * @return code 201 - created,
     *         code 400 - id is empty,
     *         code 500 - internal error
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id,
                        @Param(value = "request", required = true) final Request request) {
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        try {
            dao.upsert(key, value);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /** Delete data by key method.
     * @param id - id request.
     * @return code 202 - delited,
     *         code 400 - id is empty,
     *         code 404 - data not found,
     *         code 500 - internal error
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id) {
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        try {
            dao.remove(key);
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    public static ByteBuffer strToByteBuffer(final String msg, final Charset charset) {
        return ByteBuffer.wrap(msg.getBytes(charset));
    }

    /** Convert from ByteBuffer to Byte massive.
     *
     * @param buffer - ByteBuffer variable to convert
     */
    public static byte[] fromByteBufferToByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }
}
