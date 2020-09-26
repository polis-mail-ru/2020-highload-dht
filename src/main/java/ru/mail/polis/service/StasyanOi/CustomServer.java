package ru.mail.polis.service.StasyanOi;

import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.*;
import static one.nio.http.Request.*;

public class CustomServer extends HttpServer {

    private final DAO dao;

    public CustomServer(DAO dao, HttpServerConfig config, Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param("id") String idParam) {
        ByteBuffer id = fromBytes(idParam.getBytes(UTF_8));
        ByteBuffer response = null;
        try {
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            return Response.ok(Response.NOT_FOUND);
        }
        byte[] bytesResponse = toBytes(response);
        return Response.ok(bytesResponse);
    }

    @NotNull
    public static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    @NotNull
    public static ByteBuffer fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        return byteBuffer;
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param("id") String idParam, Request request) throws IOException {
        byte[] body = getByteArray(request.getBody());
        ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        ByteBuffer value = fromBytes(body);
        dao.upsert(key, value);
        return Response.ok(Response.CREATED);
    }

    private byte[] getByteArray(byte[] body) {
        int length = body.length;
        int mod = length % 8;
        return Arrays.copyOf(body,length + mod);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param("id") String idParam) throws IOException {
        ByteBuffer key = fromBytes(idParam.getBytes(UTF_8));
        dao.remove(key);
        return Response.ok(Response.ACCEPTED);
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
