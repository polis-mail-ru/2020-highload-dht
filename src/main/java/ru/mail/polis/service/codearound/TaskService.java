package ru.mail.polis.service.codearound;

import com.google.common.base.Charsets;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskService extends HttpServer implements Service {

    private final DAO dao;
    private final Executor exec;
    Logger logger = Logger.getLogger(TaskService.class.getName());

    /**
     * async service impl const.
     * @param port request listening port
     * @param dao DAO instance
     * @param exec thread pool executor
     */
    public TaskService(final int port, @NotNull final DAO dao, final Executor exec) throws IOException {
        super(getConfig(port));
        this.dao = dao;
        this.exec = exec;
    }

    /**
     * set HTTP server initial configuration.
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
     * returns server status info, feed to respective requests as well.
     *
     * @param id String object to be processed as a key in terms of data storage design
     * @param req client host request
     */
    @Path("/v0/entity")
    public void entity(
            @Param(value = "id", required = true) final String id, @NotNull final Request req, final HttpSession session)
            throws IOException, NoSuchMethodException {

        if (id == null || id.isEmpty()) {
            session.sendError(Response.BAD_REQUEST, "Identifier is required as parameter. Error handling request\n");
            return;
        }
        final ByteBuffer buf = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        switch (req.getMethod()) {
            case Request.METHOD_GET:
                pushAsyncRun(session, get(buf));
                break;
            case Request.METHOD_PUT:
                pushAsyncRun(session, upsert(buf, req));
                break;
            case Request.METHOD_DELETE:
                pushAsyncRun(session, delete(buf));
                break;
            default:
                throw new NoSuchMethodException("No handler provided for request method\n");
        }
    }

    /**
     * handles GET request.
     * @param key - key that should match for sending a value in server response
     * @return Response object
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
            final String faultMessage = "There is no match key paired with value to be returned. Operation failed\n";
            return new Response(Response.NOT_FOUND, faultMessage.getBytes(Charsets.UTF_8));
        }
    }

    /**
     * handles PUT request.
     * @param key - key to either initiate a new record or to modify an existing one
     * @param req client host request
     * @return server response object
     */
    private Response upsert(final ByteBuffer key, final Request req) {
        try {
            dao.upsert(key, ByteBuffer.wrap(req.getBody()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Insertion / update operation failed\n", e);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * handles DELETE request.
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
     * default request handler.
     * @param req client host request
     * @param session ongoing client-server session instance
     */
    @Override
    public void handleDefault(final Request req, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * resolves asynchronous request evaluation and handling.
     * @param session ongoing client-server session instance
     * @param response server response
     */
    public void pushAsyncRun(final HttpSession session, final Response response) {
        exec.execute(() -> {
            try {
                session.sendResponse(response);
            } catch (IOException exc) {
                try {
                    session.sendError(Response.INTERNAL_ERROR, exc.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Asynchronous execution error\n", e);
                }
            }
        });
    }

    /**
     * creates duplicate of key searched.
     * @param key key searched
     * @return key duplicate as a ByteBuffer object
     */
    public ByteBuffer duplicateValue(final ByteBuffer key) {

        ByteBuffer copy = null;

        try {
            copy = dao.get(key).duplicate();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Copying process denied as match key is missing\n", e);
        }
        return copy;
    }
}
