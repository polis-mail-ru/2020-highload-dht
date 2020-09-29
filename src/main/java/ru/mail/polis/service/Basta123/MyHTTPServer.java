package ru.mail.polis.service.basta123;

import com.google.common.net.HttpHeaders;
import one.nio.http.*;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class MyHTTPServer extends HttpServer implements Service {

    private DAO dao;

    public MyHTTPServer(HttpServerConfig config, DAO dao) throws IOException {
        super(config);
        this.dao = dao;
    }


    @Path("/abracadabra")
    @RequestMethod(Request.METHOD_GET)
    public Response abracadabraCheckMethod() {
        Response responseBadRequest = new Response(Response.BAD_REQUEST, new byte[0]);
        responseBadRequest.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
        return responseBadRequest;
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response statusCheckMethod() {
        Response responseOk = new Response(Response.OK, new byte[0]);
        responseOk.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
        return responseOk;
    }


    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response getValueByKey(@Param("id") String id) {

        if (id == null || "".equals(id)) {
            Response responseBadRequest = new Response(Response.BAD_REQUEST, new byte[0]);
            responseBadRequest.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
            return responseBadRequest;
        }

        byte[] keyBytes = id.getBytes(UTF_8);
        ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);


        ByteBuffer valueByteBuffer;
        try {
            valueByteBuffer = dao.get(keyByteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error getting value");

        } catch (NoSuchElementException e) {
            Response responseNotFound = new Response(Response.NOT_FOUND, new byte[0]);
            responseNotFound.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
            return responseNotFound;
        }

        byte[] valueBytes = getByteArrayFromByteBuffer(valueByteBuffer);
        Response responseOk = new Response(Response.OK, valueBytes);
        responseOk.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "text/plain");
        return responseOk;
    }


    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response putValueByKey(final @Param("id") String id, Request request) throws IOException {
        if ("".equals(id)) {
            Response responseBadRequest = new Response(Response.BAD_REQUEST, new byte[0]);
            responseBadRequest.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
            return responseBadRequest;
        }

        byte[] keyBytes = id.getBytes(UTF_8);
        ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        byte[] valueByte = request.getBody();
        ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueByte);

        dao.upsert(keyByteBuffer, valueByteBuffer);

        Response responseCreated = new Response(Response.CREATED, new byte[0]);
        responseCreated.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "text/plain");
        return responseCreated;
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response deleteValueByKey(@Param("id") String id) throws IOException {
        if ("".equals(id)) {
            Response responseBadRequest = new Response(Response.BAD_REQUEST, new byte[0]);
            responseBadRequest.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
            return responseBadRequest;
        }
        byte[] keyBytes = id.getBytes(UTF_8);
        ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        dao.remove(keyByteBuffer);

        Response responseAccepted = new Response(Response.ACCEPTED, new byte[0]);
        responseAccepted.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "empty");
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
