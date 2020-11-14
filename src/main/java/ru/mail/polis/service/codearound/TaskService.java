package ru.mail.polis.service.codearound;

import com.google.common.base.Charsets;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  primary HTTP server design to resolve method-specific handler running.
 */
public class TaskService extends HttpServer implements Service {

    private final DAO dao;
    private static final Logger LOGGER = Logger.getLogger(TaskService.class.getName());

    /**
     * initial (non-async) service impl const.
     *
     * @param port request listening port
     * @param dao DAO instance
     */

    public TaskService(final int port, @NotNull final DAO dao) throws IOException {
        super(TaskServerConfig.getConfig(port));
        this.dao = dao;
    }

    /**
     * fires formation request to make sure server is alive.
     */
    @Path("/v0/status")
    public Response status() {
        return new Response(Response.OK, Response.EMPTY);
    }

    /**
     * returns server status info, feed to respective requests as well.
     *
     * @param id - key either to push a new record or to modify existing one
     * @param req - client request
     * @return server response
     */
    @Path("/v0/entity")
    public Response entity(
            @Param(value = "id", required = true) final String id,
            @NotNull final Request req,
            final HttpSession session) throws IOException {
        if (id == null || id.isEmpty()) {
            session.sendError(Response.BAD_REQUEST,
                    "Identifier is required as parameter. Error handling request");
            return null;
        }
        final ByteBuffer buf = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        switch (req.getMethod()) {
            case Request.METHOD_GET:
                return get(buf);
            case Request.METHOD_PUT:
                return upsert(buf, req);
            case Request.METHOD_DELETE:
                return delete(buf);
            default:
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * GET request handler.
     *
     * @param key - key searched
     * @return server response
     */
    private Response get(final ByteBuffer key) {
        ByteBuffer copy = null;
        try {
            copy = duplicateValue(key);
            assert copy != null;
            final byte[] vals = new byte[copy.remaining()];
            copy.get(vals);
            return new Response(Response.OK, vals);
        } catch (NoSuchElementException e) {
            final String faultMessage = "There is no match key paired with value to be returned. Operation failed";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }
    }

    /**
     * PUT request handler.
     *
     * @param key - key searched
     * @param req - client request
     * @return server response
     */
    private Response upsert(final ByteBuffer key, final Request req) {
        try {
            dao.upsert(key, ByteBuffer.wrap(req.getBody()));
        } catch (IOException exc) {
            LOGGER.log(Level.SEVERE, "Insertion / update operation failed", exc);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * DELETE request handler.
     *
     * @param key - key searched
     * @return server response
     */
    private Response delete(final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException exc) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        } catch (NoSuchElementException e) {
            final String faultMessage = "There is no match key to be removed. Operation failed";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    /**
     * default handler.
     *
     * @param req - client request
     * @param session ongoing session instance
     */
    @Override
    public void handleDefault(final Request req, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * duplicates key searched.
     *
     * @param key - key searched
     * @return copy of key
     */
    public ByteBuffer duplicateValue(final ByteBuffer key) {
        ByteBuffer copy = null;
        try {
            copy = dao.get(key).duplicate();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Copying process denied as match key is missing", e);
        }
        return copy;
    }
}
