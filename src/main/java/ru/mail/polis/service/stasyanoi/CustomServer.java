package ru.mail.polis.service.stasyanoi;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class CustomServer extends HttpServer {

    private final DAO dao;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public CustomServer(final DAO dao,
                             final HttpServerConfig config,
                             final Object... routers) throws IOException {
        super(config, routers);
        this.dao = dao;
    }

    /**
     * Get a record by key.
     *
     * @param idParam - key.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public void get(final @Param("id") String idParam, final HttpSession session) {
        executorService.execute(() -> {
            try {
                getInternal(idParam, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void getInternal(final String idParam,
                             final HttpSession session) throws IOException {
        Response responseHttp;
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = new Response(Response.BAD_REQUEST);
            responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        } else {

            //get id as aligned byte buffer
            final ByteBuffer id = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            //get the response from db
            ByteBuffer body;
            try {
                body = dao.get(id);
                final byte[] bytes = toBytes(body);
                responseHttp = Response.ok(bytes);
            } catch (NoSuchElementException e) {
                //if not found then 404
                responseHttp = new Response(Response.NOT_FOUND);
                responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            }
        }

        session.sendResponse(responseHttp);
    }

    /**
     * ByteBuffer to byte array.
     *
     * @param buffer - input buffer.
     * @return byte array.
     */
    @NotNull
    public static byte[] toBytes(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    public static ByteBuffer fromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    /**
     * Create or update a record.
     *
     * @param idParam - key of the record.
     * @param request - request with the body.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public void put(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putInternal(idParam, request, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void putInternal(final String idParam, final Request request,
                             final HttpSession session) throws IOException {
        Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = new Response(Response.BAD_REQUEST);
            responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        } else {

            final ByteBuffer key = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            final ByteBuffer value = fromBytes(request.getBody());
            dao.upsert(key, value);
            responseHttp = new Response(Response.CREATED);
            responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        }
        session.sendResponse(responseHttp);
    }

    /**
     * Delete a record.
     *
     * @param idParam - key of the record to delete.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public void delete(final @Param("id") String idParam,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteInternal(idParam, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteInternal(final String idParam,
                                final HttpSession session) throws IOException {
        Response responseHttp;

        if (idParam == null || idParam.isEmpty()) {
            responseHttp = new Response(Response.BAD_REQUEST);
            responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        } else {
            final ByteBuffer key = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            dao.remove(key);
            responseHttp = new Response(Response.ACCEPTED);
            responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        }
        session.sendResponse(responseHttp);
    }

    /**
     * Abracadabra check.
     *
     * @return Response with status.
     */
    @Path("/abracadabra")
    @RequestMethod(METHOD_GET)
    public Response abracadabra() {
        final Response response = new Response(Response.BAD_REQUEST);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        final Response response = new Response(Response.OK);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
