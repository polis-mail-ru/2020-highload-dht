package ru.mail.polis.service.basta123;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class MyHttpServer extends HttpServer implements Service {

    private final DAO dao;

    public MyHttpServer(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }

    /**
     * Checking status.
     *
     * @return - return code 200 OK
     */
    @Path("/abracadabra")
    @RequestMethod(Request.METHOD_GET)
    public Response abracadabraCheckMethod() {
        Response responseBadRequest = new Response(Response.BAD_REQUEST, Response.EMPTY);
        return responseBadRequest;
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response statusCheckMethod() {
        final Response responseOk = new Response(Response.OK, new byte[0]);
        return responseOk;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getValueByKey(final @Param("id") String id) {
        if (id == null || "".equals(id)) {
            final Response responseBadRequest = new Response(Response.BAD_REQUEST, Response.EMPTY);
            return responseBadRequest;
        }

        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);


        ByteBuffer valueByteBuffer;
        try {
            valueByteBuffer = dao.get(keyByteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error getting value :", e);

        } catch (NoSuchElementException e) {
            final Response responseNotFound = new Response(Response.NOT_FOUND, Response.EMPTY);
            return responseNotFound;
        }
        final byte[] valueBytes = getByteArrayFromByteBuffer(valueByteBuffer);
        final Response responseOk = new Response(Response.OK, valueBytes);
        responseOk.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "text/plain");
        return responseOk;
    }


    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putValueByKey(final @Param("id") String id, final Request request) throws IOException {
        if ("".equals(id)) {
            final Response responseBadRequest = new Response(Response.BAD_REQUEST, Response.EMPTY);
            return responseBadRequest;
        }

        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        final byte[] valueByte = request.getBody();
        final ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueByte);

        dao.upsert(keyByteBuffer, valueByteBuffer);

        final Response responseCreated = new Response(Response.CREATED, Response.EMPTY);
        responseCreated.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "text/plain");
        return responseCreated;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteValueByKey(@Param("id") String id) throws IOException {
        if ("".equals(id)) {
            Response responseBadRequest = new Response(Response.BAD_REQUEST, Response.EMPTY);
            return responseBadRequest;
        }
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        dao.remove(keyByteBuffer);
        final Response responseAccepted = new Response(Response.ACCEPTED, Response.EMPTY);
        return responseAccepted;

    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }
}