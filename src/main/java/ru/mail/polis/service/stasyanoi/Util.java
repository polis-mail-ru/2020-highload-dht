package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import one.nio.http.*;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.Mapper;
import ru.mail.polis.service.stasyanoi.server.helpers.AckFrom;
import ru.mail.polis.service.stasyanoi.server.helpers.BodyWithTimestamp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Util {

    private final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Get response with no Body.
     *
     * @param requestType - type
     * @return - the built request.
     */
    @NotNull
    public Response responseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    /**
     * Get node with hash.
     *
     * @param nodeCount - amount of nodes.
     * @return - the node number.
     */
    public int getNode(final String idParam, final int nodeCount) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int hash = Math.abs(Arrays.hashCode(idArray));
        final int absoluteHash = hash < 0 ? -hash : hash;
        return absoluteHash % nodeCount;
    }

    /**
     * Add timestamp to body.
     *
     * @param body - body value.
     * @return - body with timestamp.
     */
    public byte[] addTimestamp(final byte[] body) {
        final byte[] timestamp = getTimestampInternal();
        final byte[] newBody = new byte[body.length + timestamp.length];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(timestamp, 0, newBody, body.length, timestamp.length);
        return newBody;
    }

    /**
     * Get current timestamp.
     *
     * @return - the timestamp.
     */
    @NotNull
    public byte[] getTimestampInternal() {
        final String nanos = String.valueOf(System.nanoTime());
        final int[] ints = nanos.chars().toArray();
        final byte[] timestamp = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            timestamp[i] = (byte) ints[i];
        }
        return timestamp;
    }

    /**
     * Add timestamp to header.
     *
     * @param timestamp - timestamp to add.
     * @param response  - the response to which to add the timestamp.
     * @return - the modified response.
     */
    public Response addTimestampHeader(final byte[] timestamp, final Response response) {
        final String timestampHeader = "Time: ";
        final StringBuilder nanoTime = new StringBuilder();
        for (byte b : timestamp) {
            nanoTime.append((char) b);
        }
        response.addHeader(timestampHeader + nanoTime);
        return response;
    }

    /**
     * Get byte buffer body.
     *
     * @param request - request from which to get the body.
     * @return - the byte buffer.
     */
    public ByteBuffer getByteBufferValue(final Request request) {
        byte[] body = request.getBody();
        body = addTimestamp(body);
        return Mapper.fromBytes(body);
    }

    /**
     * Get key from param.
     *
     * @param idParam - key param.
     * @return - key byte buffer.
     */
    @NotNull
    public ByteBuffer getKey(final String idParam) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        return Mapper.fromBytes(idArray);
    }

    /**
     * Send 503 error.
     *
     * @param errorSession - session to which to send the error.
     */
    public void send503Error(final HttpSession errorSession) {
        try {
            errorSession.sendResponse(responseWithNoBody(Response.SERVICE_UNAVAILABLE));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Get replication factor object.
     *
     * @param replicas - replicas header.
     * @param size - size of cluster.
     * @return AckFrom object.
     */
    public AckFrom getRF(final String replicas, final int size) {
        final int ack;
        final int from;
        if (replicas == null) {
            ack = size / 2 + 1;
            from = size;
        } else {
            final String pureReplicasHeader = replicas.substring(1);
            ack = Integer.parseInt(Iterables.get(Splitter.on('/').split(pureReplicasHeader), 0));
            from = Integer.parseInt(Iterables.get(Splitter.on('/').split(pureReplicasHeader), 1));
        }
        return new AckFrom(ack, from);
    }

    /**
     * Get response based on the delete time.
     *
     * @param deleteTime - delete time of the response
     * @return - the response
     */
    public Response getDeleteOrNotFoundResponse(final byte[] deleteTime) {
        if (deleteTime.length == 0) {
            return responseWithNoBody(Response.NOT_FOUND);
        } else {
            final Response deletedResponse = responseWithNoBody(Response.NOT_FOUND);
            addTimestampHeader(deleteTime, deletedResponse);
            return deletedResponse;
        }
    }

    /**
     * Get response with timestamp header.
     *
     * @param body - body with timestamp.
     * @return - the response with timestamp
     */
    public Response getResponseWithTimestamp(final ByteBuffer body) {
        final byte[] bytes = Mapper.toBytes(body);
        final BodyWithTimestamp bodyTimestamp = new BodyWithTimestamp(bytes);
        final byte[] newBody = bodyTimestamp.getPureBody();
        final byte[] time = bodyTimestamp.getTimestampObj();
        final Response okResponse = Response.ok(newBody);
        addTimestampHeader(time, okResponse);
        return okResponse;
    }

    /**
     * @param httpClient
     * @param request
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws HttpException
     * @throws PoolException
     */
    public Response sendRequestToReplicas(HttpClient httpClient, Request request)
            throws InterruptedException, IOException, HttpException, PoolException {
        final Response response;
        String newPath = request.getPath() + "/rep?" + request.getQueryString();
        if (request.getMethodName().equals("GET")) {
            response = httpClient.get(newPath);
        } else if (request.getMethodName().equals("PUT")) {
            response = httpClient.put(newPath, request.getBody());
        } else {
            response = httpClient.delete(newPath);
        }
        return response;
    }

    /**
     * @param httpClient
     * @param request
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws HttpException
     * @throws PoolException
     */
    public Response sendRequestToRemote(HttpClient httpClient, Request request)
            throws InterruptedException, IOException, HttpException, PoolException {

        String path = request.getPath();
        String queryString = request.getQueryString();
        String newPath;
        if (request.getQueryString().contains("&reps=false")) {
            newPath = path + "?" + queryString;
        } else {
            newPath = path + "?" + queryString + "&reps=false";
        }
        final Response response;
        if (request.getMethodName().equals("GET")) {
            response = httpClient.get(newPath);
        } else if (request.getMethodName().equals("PUT")) {
            response = httpClient.put(newPath, request.getBody());
        } else {
            response = httpClient.delete(newPath);
        }
        return response;
    }
}

