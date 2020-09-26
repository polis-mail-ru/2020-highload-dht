package ru.mail.polis.service.StasyanOi;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import one.nio.http.*;
import one.nio.net.Session;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import javax.mail.Header;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.*;
import static one.nio.http.Request.*;
import static one.nio.http.Response.*;

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
            Response response = new Response(BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return response;
        }

        //get id as aligned byte buffer
        ByteBuffer id = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));

        //get the response from db
        ByteBuffer response = null;
        try {
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            //if not found then 404
            Response resp = new Response(NOT_FOUND);
            resp.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return resp;
        }

        // if found then return
        byte[] bytes = dealignBuffer(toBytes(response));
        return Response.ok(bytes);
    }

    @NotNull
    public static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    private static byte[] dealignBuffer(byte[] bytes) {
        int length = bytes.length;// LENGTH - 1024

        if (length != 0) {
            byte padding = bytes[length - 1];
            if ( padding < 1 || padding > 7 ) {
                padding = 0;
            }
            return Arrays.copyOf(bytes, length - padding);
        } else {
            return bytes;
        }

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

        ByteBuffer key = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));
        ByteBuffer value = fromBytes(alignByteArray(request.getBody()));
        dao.upsert(key, value);
        Response response = new Response(CREATED);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    private byte[] alignByteArray(byte[] body) {
            int length = body.length;
            int mod = getDiv8(length);
            byte[] newBody = Arrays.copyOf(body, mod);
            if (mod != length) {
                newBody[newBody.length - 1] = (byte) (mod - length);
            }
            return newBody;
    }

    private int getDiv8(int length) {
        while (length % 8 != 0){
            ++length;
        }
        return length;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param("id") String idParam) throws IOException {
        if (idParam == null || idParam.isEmpty()) {
            Response response = new Response(BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": 0");
            return response;
        }
        ByteBuffer key = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));
        dao.remove(key);
        Response response = new Response(ACCEPTED);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
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
