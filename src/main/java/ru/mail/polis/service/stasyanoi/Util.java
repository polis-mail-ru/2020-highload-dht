package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

public final class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Get response with no Body.
     *
     * @param requestType - type
     * @return - the built request.
     */
    @NotNull
    public static Response responseWithNoBody(final String requestType) {
        return new Response(requestType, Response.EMPTY);
    }

    /**
     * Get node with hash.
     *
     * @param nodeCount - amount of nodes.
     * @return - the node number.
     */
    public static int getNode(final String idParam, final int nodeCount) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int absoluteHash = getAbsoluteHash(idArray);
        return absoluteHash % nodeCount;
    }

    private static int getAbsoluteHash(final byte[] idArray) {
        final int hash = Arrays.hashCode(idArray) % Constants.HASH_THRESHOLD;
        return hash < 0 ? -hash : hash;
    }

    /**
     * Add timestamp to body and abb byte to empty body.
     *
     * @param bodyTemp - body value.
     * @return - body with timestamp.
     */
    public static byte[] addTimestampToBodyAndModifyEmptyBody(final byte[] bodyTemp) {
        final byte[] body = addByteIfEmpty(bodyTemp);
        final byte[] timestamp = getTimestampInternal();
        final byte[] newBody = new byte[body.length + timestamp.length];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(timestamp, 0, newBody, body.length, timestamp.length);
        return newBody;
    }

    @NotNull
    private static byte[] addByteIfEmpty(final byte[] body) {
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
    public static byte[] getTimestampInternal() {
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
    public static Response addTimestampHeaderToResponse(final byte[] timestamp, final Response response) {
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
    public static ByteBuffer getKey(final String idParam) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        return Mapper.fromBytes(idArray);
    }

    /**
     * Send 503 error.
     *
     * @param errorSession - session to which to send the error.
     */
    public static void send503Error(final HttpSession errorSession) {
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
    public static AckFrom getRF(final String replicas, final int size) {
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
    public static Response getResponseWithTimestamp(final ByteBuffer body) {
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
     * Get one nio response.
     *
     * @param javaResponse - response.
     * @return one nio response.
     */
    public static Response getOneNioResponse(final HttpResponse<byte[]> javaResponse) {
        final Response response = new Response(String.valueOf(javaResponse.statusCode()), javaResponse.body());
        javaResponse.headers().map().forEach((s, strings) -> response.addHeader(s + ": " + strings.get(0)));
        return response;
    }

    /**
     * Filter responses for throwables.
     *
     * @param response  - response received.
     * @param throwable - throwable that has been thrown.
     * @return the response after filtration.
     */
    public static Response filterResponse(final Response response, final Throwable throwable) {
        if (throwable == null) {
            return response;
        } else {
            return responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    /**
     * Get net java request.
     *
     * @param request - one nio request.
     * @param host - host.
     * @return - java net request.
     */
    public static HttpRequest getJavaRequest(final Request request, final String host) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        String newPath;
        if (request.getQueryString().contains("&reps=false")) {
            newPath = request.getPath() + "?" + request.getQueryString();
        } else {
            newPath = request.getPath() + "?" + request.getQueryString() + "&reps=false";
        }
        final String uri = host + newPath;
        final String methodName = request.getMethodName();
        final HttpRequest.Builder requestBuilder = builder
                .timeout(Duration.ofSeconds(1))
                .uri(URI.create(uri));
        if ("GET".equalsIgnoreCase(methodName)) {
            return requestBuilder.GET().build();
        } else if ("PUT".equalsIgnoreCase(methodName)) {
            final HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(request
                    .getBody());
            return requestBuilder.PUT(bodyPublisher).build();
        } else {
            return requestBuilder.DELETE().build();
        }
    }

    /**
     * Checks if the replication factor is valid. (number/number)
     *
     * @param replicationParam - replication parameter
     * @return true if valid else false
     */
    public static boolean validRF(final String replicationParam) {
        if (replicationParam == null) {
            return true;
        } else {
            final String pureRF = replicationParam.substring(1);
            try {
                final Iterable<String> splitParam = Splitter.on('/').split(pureRF);
                Integer.parseInt(Iterables.get(splitParam, 0));
                Integer.parseInt(Iterables.get(splitParam, 1));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}


