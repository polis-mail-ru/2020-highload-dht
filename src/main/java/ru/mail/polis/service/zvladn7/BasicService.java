package ru.mail.polis.service.zvladn7;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class BasicService extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(BasicService.class);

    private final DAO dao;
    private final LinkedHashMap<String, byte[]> cache = new LinkedHashMap<>();

    public BasicService(final int port,
                        @NotNull final DAO dao) throws IOException {
        super(provideConfig(port));
        this.dao = dao;
    }

    private static HttpServerConfig provideConfig(final int port) {
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};

        return config;
    }

    private static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    private static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }

        return Response.EMPTY;
    }

    private static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    private static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        log.error("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("Status: OK");
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(@Param(value = "id", required = true) final String  id) {
        log.debug("GET request with mapping: /v0/entity and key={}", id);
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        byte[] body = cache.get(id);
        if (body == null) {
            final ByteBuffer key = wrapString(id);
            final ByteBuffer value;

            try {
                value = dao.get(key);
            } catch (NoSuchElementException ex) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } catch (IOException e) {
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }

            body = toBytes(value);
            if (cache.size() >= 10) {
                Iterator<Map.Entry<String, byte[]>> iterator = cache.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            cache.put(id, body);
        }

        return Response.ok(body);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response remove(@Param(value = "id", required = true) final String  id) {
        log.debug("DELETE request with mapping: /v0/entity and key={}", id);
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer key = wrapString(id);

        try {
            dao.remove(key);
            cache.remove(id);
        } catch (IOException e) {
            return  new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response upsert(
            @Param(value = "id", required = true) final String  id,
            Request request) {
        log.debug("PUT request with mapping: /v0/entity with: key={}, value={}",
                id, new String(request.getBody(), StandardCharsets.UTF_8));
        if (id.isEmpty()) {
            return new Response(Response.BAD_REQUEST, Response.EMPTY);
        }

        final ByteBuffer key = wrapString(id);
        final ByteBuffer value = wrapArray(request.getBody());

        try {
            dao.upsert(key, value);
            cache.put(id, request.getBody());
        } catch (IOException e) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }


}
