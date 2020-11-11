package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
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
        return new Response(requestType, Response.EMPTY);
    }

    /**
     * Get node with hash.
     *
     * @param nodeCount - amount of nodes.
     * @return - the node number.
     */
    public int getNode(final String idParam, final int nodeCount) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int absoluteHash = getAbsoluteHash(idArray);
        return absoluteHash % nodeCount;
    }

    private int getAbsoluteHash(final byte[] idArray) {
        final int hash = Arrays.hashCode(idArray) % Constants.HASH_THRESHOLD;
        return hash < 0 ? -hash : hash;
    }

    /**
     * Add timestamp to body and abb byte to empty body.
     *
     * @param bodyTemp - body value.
     * @return - body with timestamp.
     */
    public byte[] addTimestampToBodyAndModifyEmptyBody(byte[] bodyTemp) {
        final byte[] body = addByteIfEmpty(bodyTemp);
        final byte[] timestamp = getTimestampInternal();
        final byte[] newBody = new byte[body.length + timestamp.length];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(timestamp, 0, newBody, body.length, timestamp.length);
        return newBody;
    }

    @NotNull
    private byte[] addByteIfEmpty(final byte[] body) {
        if (body.length == 0) {
            return new byte[1];
        }
        return body;
    }

    /**
     * Get current timestamp.
     *
     * @return - the timestamp.
     */
    @NotNull
    public byte[] getTimestampInternal() {
        final String nanos = String.valueOf(System.currentTimeMillis());
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
    public Response addTimestampHeaderToResponse(final byte[] timestamp, final Response response) {
        final StringBuilder nanoTime = new StringBuilder();
        for (final byte b : timestamp) {
            nanoTime.append((char) b);
        }
        response.addHeader(Constants.TIMESTAMP_HEADER_NAME + nanoTime);
        return response;
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
     * Get response with timestamp header.
     *
     * @param body - body with timestamp.
     * @return - the response with timestamp
     */
    public Response getResponseWithTimestamp(final ByteBuffer body) {
        final byte[] bytes = Mapper.toBytes(body);
        final BodyWithTimestamp bodyTimestamp = new BodyWithTimestamp(bytes);
        final byte[] newBody;
        if (bytes.length == Constants.EMPTY_BODY_SIZE) {
            newBody = new byte[0];
        } else {
            newBody = bodyTimestamp.getPureBody();
        }
        final byte[] time = bodyTimestamp.getTimestamp();
        return addTimestampHeaderToResponse(time, Response.ok(newBody));
    }

    /**
     * Send request for sharding or replication.
     *
     * @param httpClient - client to use.
     * @param request - request to send.
     * @return received response.
     * @throws InterruptedException - thrown if interrupted.
     * @throws IOException - thrown if network io exception occurs in the client.
     * @throws HttpException - thrown if http protocol exception encountered.
     * @throws PoolException - thrown if problems with pooling occurs.
     */
    public Response sendRequestInternal(final HttpClient httpClient, final Request request)
            throws InterruptedException, PoolException, IOException, HttpException {
        final Response response;
        String newPath;
        if (request.getQueryString().contains("&reps=false")) {
            newPath = request.getPath() + "?" + request.getQueryString();
        } else {
            newPath = request.getPath() + "?" + request.getQueryString() + "&reps=false";
        }
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

