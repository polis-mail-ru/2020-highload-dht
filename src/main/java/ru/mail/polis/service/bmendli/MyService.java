package ru.mail.polis.service.bmendli;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
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
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

public class MyService extends HttpServer implements Service {

    @NotNull
    private final DAO dao;

    public MyService(final int port, @NotNull DAO dao) throws IOException {
        super(createConfigFromPort(port));
        this.dao = dao;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@NotNull @Param(required = true, value = "id") final String id) {
        try {
            final Response response = handleParam(id);
            return response == null
                    ? Response.ok(getBytesFromByteBuffer(dao.get(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)))))
                    : response;
        } catch (NoSuchElementException nsee) {
            nsee.printStackTrace();
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(@NotNull @Param(required = true, value = "id") final String id, @NotNull final Request request) {
        try {
            final Response response = handleParam(id);
            if (response != null) {
                return response;
            }
            dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            e.printStackTrace();
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@NotNull @Param(required = true, value = "id") final String id) {
        try {
            final Response response = handleParam(id);
            if (response != null) {
                return response;
            }
            dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            e.printStackTrace();
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.EMPTY);
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Nullable
    private static Response handleParam(@NotNull final String param) {
        return param.isBlank()
                ? new Response(Response.BAD_REQUEST, Response.EMPTY)
                : null;
    }

    @NotNull
    private static byte[] getBytesFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    @NotNull
    private static HttpServerConfig createConfigFromPort(final int port) {
        AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.selectors = 4;
        return config;
    }
}
