package ru.mail.polis.service.alexander.marashov;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.mgt.Management;
import one.nio.server.AcceptorConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class ServiceImpl extends HttpServer implements Service {

    private static final Log log = LogFactory.getLog(Management.class);
    private final DAO dao;

    /**
     * Implementation of a persistent storage with HTTP API.
     * @author Marashov Alexander
     */
    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(configFrom(port));
        BasicConfigurator.configure();
        this.dao = dao;
    }

    /**
     * Static function for creating an {@link HttpServerConfig} instance
     * @param port port on which the {@link HttpServer} should listen
     * @return {@link HttpServerConfig} for {@link HttpServer} with specified port
     */
    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    /**
     * Static function for converting {@link ByteBuffer} object to simple byte[] array
     * @param buffer {@link ByteBuffer} instance that needs to be converted
     * @return byte[] array with buffer's data or empty array if buffer is empty
     */
    @NotNull
    private static byte[] getBytes(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    /**
     * Http method handler for checking server's reachability
     * @return {@link Response} with status {@code 200} if the server is available
     */
    @Path("/v0/status")
    public Response handleStatus() {
        log.debug("Status method: OK");
        return new Response(Response.OK, Response.EMPTY);
    }

    /**
     * Http method handler for getting a value in the DAO by the key
     * @param id is the key for searching for a value in the DAO
     * @return {@link Response} instance with value as body, if the key exists,
     * Response status is {@code 200} if data is found
     * {@code 400} if id is empty
     * {@code 404} if not found,
     * {@code 500} if an internal server error occurred
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response handleEntityGet(@Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            log.debug("Get entity method: key is empty");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        log.debug(String.format("Get entity method: key = '%s'", id));

        final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer key = ByteBuffer.wrap(bytes);

        final ByteBuffer result;
        try {
            result = this.dao.get(key);
        } catch (NoSuchElementException e) {
            log.debug(String.format("Get entity method: key = '%s' not found", id));
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException e) {
            log.error(String.format("Get entity method: key = '%s' error", id), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        log.debug(String.format("Get entity method: key = '%s', value = '%s'", id, result.toString()));
        return new Response(Response.OK, getBytes(result));
    }

    /**
     * HTTP method handler for placing a value by the key in the DAO storage
     * @param id is the key that the data will be associated with
     * @return {@link Response} instance with
     * {@code 201} if data saved
     * {@code 400} if id is empty,
     * {@code 500} if an internal server error occurred,
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response handleEntityPut(final Request request, @Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            log.debug("Put entity method: key is empty");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer key = ByteBuffer.wrap(bytes);

        final byte[] body = request.getBody();
        final ByteBuffer value = ByteBuffer.wrap(body);

        log.debug(String.format("Put entity method: key = '%s', body = %s", id, Arrays.toString(body)));
        try {
            this.dao.upsert(key, value);
        } catch (IOException e) {
            log.error(String.format("Put entity method: key = '%s', value = '%s' error", id, Arrays.toString(body)), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        log.debug(String.format("Put entity method: key = '%s', value = '%s' OK", id, Arrays.toString(body)));
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * HTTP method handler for removing a value by the key from the DAO storage
     * @param id is the key that the data associated with
     * @return {@link Response} instance with
     * {@code 202} if the key deleted,
     * {@code 400} if id is empty,
     * {@code 500} if an internal server error occurred
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response handleEntityDelete(@Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            log.debug("Delete entity method: key is empty");
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }
        log.debug(String.format("Delete entity method: key = '%s'", id));

        final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer key = ByteBuffer.wrap(bytes);

        try {
            this.dao.remove(key);
        } catch (IOException e) {
            log.error(String.format("Delete entity method: key = '%s' error", id), e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        log.debug(String.format("Delete entity method: key = '%s' removed", id));
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
