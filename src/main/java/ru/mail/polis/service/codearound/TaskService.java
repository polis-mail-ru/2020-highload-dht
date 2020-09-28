package ru.mail.polis.service.codearound;

import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import com.google.common.base.Charsets;
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

    /**
     * define HTTP server configuration parameters initially.
     * @param port - server listening port
     * @return HTTP server configuration object
     */

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

    /**
     * try formation request to secure OK as response.
     */
    @Path("/v0/status")
    public Response status() {
        return new Response(Response.OK, Response.EMPTY);
    }

    /**
     * determine what request and how to be handled if receiving one.
     *
     * @param id String object to be processed as a key in terms of data storage design
     * @param req client host request
     * @return a byte array object
     */
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
     * handle GET request.
     * @param key - key that should match for sending a value in server response
     * @return Response object
     */

    private Response get(final ByteBuffer key) throws IOException {
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

    /**
     * handle PUT request.
     * @param key - key to either initiate a new record or to modify an existing one
     * @param req client host request
     * @return server response object
     */
    private Response insert(final ByteBuffer key, final Request req) throws IOException {
        dao.upsert(key, ByteBuffer.wrap(req.getBody()));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * handle DELETE request.
     * @param key - specific key to remove a record from storage unless match is missing there
     * @return server response object
     */
    private Response delete(final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            final String faultMessage = "There is no match key to be removed. Operation failed\n";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    /**
     * set default handler
     * @param req client host request
     * @param session ongoing client-server session instance
     */
    @Override
    public void handleDefault(final Request req, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
