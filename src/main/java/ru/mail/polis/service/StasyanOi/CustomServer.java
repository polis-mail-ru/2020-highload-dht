package ru.mail.polis.service.StasyanOi;

import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
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
        ByteBuffer id = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));
        ByteBuffer response = null;
        try {
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            return Response.ok(Response.NOT_FOUND);
        }
        byte[] bytesResponse = toBytes(response);
        byte[] bytes = dealignBuffer(bytesResponse);
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
        int length = bytes.length;

        byte padding = bytes[length - 1];
        if (padding > 7) {
            padding = 0;
        }
        return Arrays.copyOf(bytes, length - padding);
    }

    @NotNull
    public static ByteBuffer fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param("id") String idParam, Request request) throws IOException {
        ByteBuffer key = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));
        ByteBuffer value = fromBytes(alignByteArray(request.getBody()));
        dao.upsert(key, value);
        return Response.ok(Response.CREATED);
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
        ByteBuffer key = fromBytes(alignByteArray(idParam.getBytes(UTF_8)));
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
