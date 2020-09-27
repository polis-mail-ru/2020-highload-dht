package ru.mail.polis.service.stasyanoi;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class CustomServer extends HttpServer {

    private final DAO dao;

    public CustomServer(final DAO dao,
                        final HttpServerConfig config,
                        final Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(final @Param("id") String idParam) {
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            final Response badReqResponse = new Response(Response.BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return badReqResponse;
        }

        //get id as aligned byte buffer
        final ByteBuffer id = fromBytes(idParam.getBytes(UTF_8));

        //get the response from db
        ByteBuffer response;
        try {
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            //if not found then 404
            final Response notFoundResponse = new Response(Response.NOT_FOUND);
            notFoundResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return notFoundResponse;
        }

        // if found then return
        final byte[] bytes = toBytes(response);
        return Response.ok(bytes);
    }

    @NotNull
    public static byte[] toBytes(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    public static ByteBuffer fromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(final @Param("id") String idParam,
                        final Request request) throws IOException {
        if (idParam == null || idParam.isEmpty()) {
            final Response response = new Response(Response.BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return response;
        }

        final ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        final ByteBuffer value = fromBytes(request.getBody());
        dao.upsert(key, value);
        final Response response = new Response(Response.CREATED);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(final @Param("id") String idParam) throws IOException {
        if (idParam == null || idParam.isEmpty()) {
            final Response badReqResponse = new Response(Response.BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return badReqResponse;
        }

        final ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        dao.remove(key);

        final Response acceptedResponse = new Response(Response.ACCEPTED);
        acceptedResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return acceptedResponse;
    }

    @Path("/abracadabra")
    @RequestMethod(METHOD_GET)
    public Response abracadabra() {
        final Response response = new Response(Response.BAD_REQUEST);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        final Response response = new Response(Response.OK);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
