package ru.mail.polis.service.mariarheon;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class ServiceImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(ServiceImpl.class);

    @NotNull
    private final DAO dao;

    public ServiceImpl(final HttpServerConfig config,
                       @NotNull final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }

    /** Get data by key.
     * @param key - record id.
     * @return 200 OK + data,
     *         400 Bad Request,
     *         404 Not Found,
     *         500 Internal Server Error.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(final @Param(value = "id", required = true) String key) {
        if (key.isEmpty()) {
            logger.info("ServiceImpl.get() method: key is empty");
            return new ZeroResponse(Response.BAD_REQUEST);
        }
        try {
            final ByteBuffer response = dao.get(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)));
            return Response.ok(ByteBufferUtils.toArray(response));
        } catch (NoSuchElementException ex) {
            return new ZeroResponse(Response.NOT_FOUND);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.get() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
    }

    /** Insert or change key-data pair.
     * @param key - record id.
     * @param request - record data.
     * @return 200 Created,
     *         400 Bad Request,
     *         500 Internal Server Error.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(final @Param(value = "id", required = true) String key,
                        final @Param("request") Request request) {
        if (key.isEmpty()) {
            logger.info("ServiceImpl.put() method: key is empty");
            return new ZeroResponse(Response.BAD_REQUEST);
        }
        try {
            dao.upsert(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)),
                    ByteBufferUtils.toByteBuffer(request.getBody()));
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.put() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
        return new ZeroResponse(Response.CREATED);
    }

    /** Remove key-data pair.
     * @param key - record id.
     * @return 202 Accepted,
     *         400 Bad Request,
     *         404 Not Found,
     *         500 Internal Server Error.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(final @Param(value = "id", required = true) String key) {
        if (key.isEmpty()) {
            logger.info("ServiceImpl.delete() method: key is empty");
            return new ZeroResponse(Response.BAD_REQUEST);
        }
        try {
            dao.remove(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchElementException ex) {
            return new ZeroResponse(Response.NOT_FOUND);
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.delete() method; internal error: ", ex);
            return new ZeroResponse(Response.INTERNAL_ERROR);
        }
        return new ZeroResponse(Response.ACCEPTED);
    }

    @Path("/v0/status")
    public Response status() {
        return new ZeroResponse(Response.OK);
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new ZeroResponse(Response.BAD_REQUEST));
    }

}
