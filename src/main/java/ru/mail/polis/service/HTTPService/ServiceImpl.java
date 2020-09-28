package ru.mail.polis.service.HTTPService;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.*;

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

    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String id){
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        final ByteBuffer val;
        try{
             val = dao.get(key);
        }catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }catch(IOException e){
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        return Response.ok(fromByteBufferToByte(val));
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(@Param(value = "id", required = true) final String id,
                        @Param(value = "value", required = true) final Request request){
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        try{
            dao.upsert(key, value);
        }catch(IOException e){
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) final String id){
        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        final ByteBuffer key = strToByteBuffer(id, UTF_8);
        try{
            dao.remove(key);
        }catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }catch(IOException e){
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
        return new Response (Response.ACCEPTED, Response.EMPTY);
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
    public static ByteBuffer strToByteBuffer(String msg, Charset charset){
        return ByteBuffer.wrap(msg.getBytes(charset));
    }

    public static byte[] fromByteBufferToByte(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufferCopy = buffer.duplicate();
        final byte[] array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }


}
