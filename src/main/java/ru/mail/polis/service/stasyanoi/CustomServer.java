package ru.mail.polis.service.stasyanoi;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
     * @return Response with the value.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public Response get(final @Param("id") String idParam) {
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            final Response badReqResponse = new Response(Response.BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return badReqResponse;
        }

        //get id as aligned byte buffer
        final ByteBuffer id = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));

        //get the response from db
        ByteBuffer response;
        try {
            final Future<ByteBuffer> future = executorService.submit(() -> dao.get(id));
           Awaitility.await().pollInterval(0, TimeUnit.NANOSECONDS).until(future::isDone);
            response = dao.get(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            //if not found then 404
            final Response notFoundResponse = new Response(Response.NOT_FOUND);
            notFoundResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return notFoundResponse;
        }

        // if found then return
        final byte[] bytes = toBytes(response);
        return Response.ok(bytes);
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
     * @return Response with a status.
     * @throws IOException - read/write file exception.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public Response put(final @Param("id") String idParam,
                        final Request request) throws ExecutionException, InterruptedException {
        if (idParam == null || idParam.isEmpty()) {
            final Response response = new Response(Response.BAD_REQUEST);
            response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return response;
        }

        final ByteBuffer key = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
        final ByteBuffer value = fromBytes(request.getBody());
        final Future<?> future = executorService.submit(() -> {
            try {
                dao.upsert(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Awaitility.await().pollInterval(0, TimeUnit.NANOSECONDS).until(future::isDone);
        future.get();
        final Response response = new Response(Response.CREATED);
        response.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return response;
    }

    /**
     * Delete a record.
     *
     * @param idParam - key of the record to delete.
     * @return Response with a status.
     * @throws IOException - file read/write exception
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public Response delete(final @Param("id") String idParam) throws ExecutionException, InterruptedException {
        if (idParam == null || idParam.isEmpty()) {
            final Response badReqResponse = new Response(Response.BAD_REQUEST);
            badReqResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
            return badReqResponse;
        }

        final ByteBuffer key = fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
        final Future<?> future = executorService.submit(() -> {
            try {
                dao.remove(key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
       Awaitility.await().pollInterval(0, TimeUnit.NANOSECONDS).until(future::isDone);
        future.get();

        final Response acceptedResponse = new Response(Response.ACCEPTED);
        acceptedResponse.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return acceptedResponse;
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
