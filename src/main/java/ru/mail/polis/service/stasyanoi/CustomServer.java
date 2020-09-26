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
import static one.nio.http.Response.ACCEPTED;
import static one.nio.http.Response.BAD_REQUEST;
import static one.nio.http.Response.CREATED;
import static one.nio.http.Response.NOT_FOUND;
import static one.nio.http.Response.OK;
import static one.nio.http.Response.ok;

public class CustomServer extends HttpServer {

    private final DAO dao;

    public CustomServer(DAO dao, HttpServerConfig config, Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param("id") String idParam) {
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            Response badReqResponse = new Response(BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return badReqResponse;
        }

        //get id as aligned byte buffer
        ByteBuffer id = fromBytes(idParam.getBytes(UTF_8));

        //get the response from db
        ByteBuffer response;
        try {
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            //if not found then 404
            Response notFoundResponse = new Response(NOT_FOUND);
            notFoundResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return notFoundResponse;
        }

        // if found then return
        byte[] bytes = toBytes(response);
        return ok(bytes);
    }

    @NotNull
    public static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    @NotNull
    public static ByteBuffer fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param("id") String idParam, Request request) throws IOException {
        if (idParam == null || idParam.isEmpty()) {
            Response response = new Response(BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return response;
        }

        ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        ByteBuffer value = fromBytes(request.getBody());
        dao.upsert(key, value);
        Response response = new Response(CREATED);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param("id") String idParam) throws IOException {
        if (idParam == null || idParam.isEmpty()) {
            Response badReqResponse = new Response(BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return badReqResponse;
        }

        ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        dao.remove(key);

        Response acceptedResponse = new Response(ACCEPTED);
        acceptedResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return acceptedResponse;
    }

    @Path("/abracadabra")
    @RequestMethod(METHOD_GET)
    public Response abracadabra() {
        Response response = new Response(BAD_REQUEST);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        Response response = new Response(OK);
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
