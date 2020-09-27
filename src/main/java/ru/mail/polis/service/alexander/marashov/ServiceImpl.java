package ru.mail.polis.service.alexander.marashov;

import one.nio.http.*;
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

    public ServiceImpl(final int port, @NotNull final DAO dao) throws IOException {
        super(configFrom(port));
        BasicConfigurator.configure();
        this.dao = dao;
    }

    @NotNull
    private static HttpServerConfig configFrom(final int port) {
        AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;

        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    private static byte[] getBytes(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    @Path("/v0/status")
    public Response handleStatus() {
        log.debug("Status method: OK");
        return new Response(Response.OK, Response.EMPTY);
    }

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

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response handleEntityPut(@Param(value = "id", required = true) final String id, Request request) {
        if (id.isEmpty()) {
            log.debug("Get entity method: key is empty");
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

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response handleEntityDelete(@Param(value = "id", required = true) final String id) {
        if (id.isEmpty()) {
            log.debug("Get entity method: key is empty");
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

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
