package ru.mail.polis.service.codearound;
import com.google.common.base.Charsets;
import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
public class TaskService extends HttpServer implements Service {

    private final DAO dao;
    public TaskService(final int port, @NotNull final DAO dao) throws IOException {
        super(getConfig(port));
        this.dao = dao;
    }
    private static HttpServerConfig getConfig(final int port) {
        if (port <= 1024 || port >= 65536) {
            throw new IllegalArgumentException("Invalid port");
        }
        final AcceptorConfig acc = new AcceptorConfig();
        final HttpServerConfig config = new HttpServerConfig();
        acc.port = port;
        acc.deferAccept = true;
        acc.reusePort = true;

        config.acceptors = new AcceptorConfig[]{acc};
        return config;
    }
    @Path("/v0/status")
    public Response status() {
        return new Response(Response.OK, Response.EMPTY);
    }

    @Path("/v0/entity")
    public Response entity(@Param(value = "id", required = true) final String id, @NotNull final Request req) {

        if (id == null || id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer buf = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        try {
            switch (req.getMethod()) {
                case Request.METHOD_GET:
                    return get(buf);
                case Request.METHOD_PUT:
                    return insert(buf, req);
                case Request.METHOD_DELETE:
                    return delete(buf);
                default:
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        } catch (IOException exception) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Getting a response entity for path "/v0/entity".
     *
     * @param id      type String
     * @param request type Request
     * @return Response object
     */

    private Response get(final ByteBuffer key) throws IOException {
        //byte [] keyArray = new byte[key.remaining()];
        //key.get(keyArray);
        //String keyStr = new String(keyArray, StandardCharsets.UTF_8);

        try {
            final ByteBuffer copy = dao.get(key).duplicate();
            final byte[] vals = new byte[copy.remaining()];
            copy.get(vals);
            return new Response(Response.OK, vals);
        } catch (NoSuchElementException e) {
            String faultMessage = "There is no match key paired with value to be returned. Operation failed\n";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }
    }

    private Response insert(final ByteBuffer key, final Request request) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(request.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Response delete(final ByteBuffer key) {
        //byte [] keyArray = new byte[key.remaining()];
        //key.get(keyArray);
        //String keyStr = new String(keyArray, StandardCharsets.UTF_8);

        try {
            dao.remove(key);
        } catch(IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            String faultMessage = "There is no match key to be removed. Operation failed\n";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}